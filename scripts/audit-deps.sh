#!/usr/bin/env bash
# Fails if a module declares a libs.versions.toml dependency (implementation/api,
# not test-only) that has no corresponding `import` under its src/main tree.
# axiom.md §9: "nenhuma dependência declarada sem uso confirmado por import real".
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
catalog="$root/gradle/libs.versions.toml"
status=0

# Some libraries' Maven groupId doesn't match their Java package prefix (Gson is
# "com.google.code.gson" as a coordinate but "com.google.gson" as a package). Override the
# import-prefix lookup per-alias here rather than assuming groupId == package root.
declare -A alias_import_prefix=(
    [gson]="com.google.gson"
)

# alias -> module ("group:artifact") from the [libraries] table
declare -A alias_module
in_libraries=0
while IFS= read -r line; do
    if [[ "$line" =~ ^\[libraries\] ]]; then in_libraries=1; continue; fi
    if [[ "$line" =~ ^\[ ]]; then in_libraries=0; continue; fi
    if [[ $in_libraries -eq 1 && "$line" =~ ^([a-zA-Z0-9]+)[[:space:]]*=.*module[[:space:]]*=[[:space:]]*\"([^\"]+)\" ]]; then
        alias_module["${BASH_REMATCH[1]}"]="${BASH_REMATCH[2]}"
    fi
done < "$catalog"

for module_dir in "$root"/axiom-*/; do
    [ -f "$module_dir/build.gradle.kts" ] || continue
    module="$(basename "$module_dir")"
    src_main="$module_dir/src/main/java"

    # aliases referenced by implementation(...)/api(...), i.e. real compile-time deps
    used_aliases=$(grep -oE '(implementation|api)\(libs\.[a-zA-Z0-9]+\)' "$module_dir/build.gradle.kts" 2>/dev/null \
        | sed -E 's/.*libs\.([a-zA-Z0-9]+)\)/\1/' || true)

    for alias in $used_aliases; do
        module_coord="${alias_module[$alias]:-}"
        if [ -z "$module_coord" ]; then
            echo "::error:: $module declares libs.$alias but it has no entry in gradle/libs.versions.toml"
            status=1
            continue
        fi
        group="${alias_import_prefix[$alias]:-${module_coord%%:*}}"
        # look for an import rooted at the dependency's package prefix anywhere in src/main
        if [ -d "$src_main" ] && grep -rqE "^import ${group}(\.[a-zA-Z0-9_]+)*" "$src_main" 2>/dev/null; then
            continue
        fi
        echo "::error:: $module declares libs.$alias ($module_coord) but no import under $group.* found in $src_main"
        status=1
    done
done

if [ $status -eq 0 ]; then
    echo "Dependency audit passed: every declared implementation/api dependency has a confirmed import."
fi

exit $status

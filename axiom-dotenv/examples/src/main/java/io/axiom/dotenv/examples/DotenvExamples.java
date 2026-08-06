package io.axiom.dotenv.examples;

import io.axiom.dotenv.Default;
import io.axiom.dotenv.Dotenv;
import io.axiom.dotenv.DotenvBinder;
import io.axiom.dotenv.Env;
import io.axiom.dotenv.EnvPrefix;

/** Minimal, compiled-by-CI usage examples for {@code axiom-dotenv}. */
public final class DotenvExamples {

    private DotenvExamples() {
    }

    @EnvPrefix("APP_")
    static class AppConfig {
        @Env("HOST")
        @Default("localhost")
        String host;

        @Env("PORT")
        @Default("8080")
        int port;
    }

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .load();

        AppConfig config = new AppConfig();
        DotenvBinder.bind(config, dotenv);

        System.out.println("host: " + config.host);
        System.out.println("port: " + config.port);
    }
}

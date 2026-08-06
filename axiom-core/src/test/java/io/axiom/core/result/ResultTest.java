package io.axiom.core.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    @Test
    void okHoldsValue() {
        Result<Integer, String> r = Result.ok(10);
        assertTrue(r.isOk());
        assertEquals(10, r.orElse(0));
    }

    @Test
    void errHoldsError() {
        Result<Integer, String> r = Result.err("bad input");
        assertTrue(r.isErr());
        assertEquals("bad input", r.errorOrThrow());
    }

    @Test
    void mapTransformsOkAndPreservesErr() {
        Result<Integer, String> ok = Result.<Integer, String>ok(10).map(n -> n * 2);
        assertEquals(20, ok.orElse(-1));

        Result<Integer, String> err = Result.<Integer, String>err("boom").map(n -> n * 2);
        assertTrue(err.isErr());
    }

    @Test
    void flatMapChainsAndPropagatesErr() {
        Result<Integer, String> chained = Result.<Integer, String>ok(10)
                .flatMap(n -> n == 0 ? Result.err("div by zero") : Result.ok(100 / n));

        assertEquals(10, chained.orElse(-1));

        Result<Integer, String> propagated = Result.<Integer, String>err("upstream failure")
                .flatMap(n -> Result.ok(n * 2));

        assertTrue(propagated.isErr());
        assertEquals("upstream failure", propagated.errorOrThrow());
    }

    @Test
    void recoverConvertsErrToOk() {
        Result<Integer, String> recovered = Result.<Integer, String>err("bad").recover(err -> 0);
        assertTrue(recovered.isOk());
        assertEquals(0, recovered.orElse(-1));
    }

    @Test
    void foldHandlesBothBranches() {
        String okMsg = Result.<Integer, String>ok(5).fold(v -> "ok:" + v, e -> "err:" + e);
        String errMsg = Result.<Integer, String>err("x").fold(v -> "ok:" + v, e -> "err:" + e);

        assertEquals("ok:5", okMsg);
        assertEquals("err:x", errMsg);
    }

    @Test
    void orElseThrowThrowsForErr() {
        Result<Integer, String> err = Result.err("bad");
        assertThrows(IllegalStateException.class, err::orElseThrow);
    }

    @Test
    void toOptionalReflectsOkAndErr() {
        assertTrue(Result.ok("v").toOptional().isPresent());
        assertFalse(Result.<String, String>err("e").toOptional().isPresent());
    }
}

package io.axiom.console.print;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class IOTest {

    @AfterEach
    void resetGlobalState() {
        IO.use(System.out);
        IO.setStylingEnabled(null);
    }

    @Test
    void substitutesPositionalPlaceholders() {
        assertEquals("Hello Ada, you are 30", IO.render("Hello {}, you are {}", "Ada", 30));
    }

    @Test
    void tooFewArgumentsThrows() {
        assertThrows(IllegalArgumentException.class, () -> IO.render("Hello {}, {}", "Ada"));
    }

    @Test
    void tooManyArgumentsThrows() {
        assertThrows(IllegalArgumentException.class, () -> IO.render("Hello {}", "Ada", "extra"));
    }

    @Test
    void stripsTagsWhenDisabled() {
        IO.setStylingEnabled(false);
        assertEquals("this is green", IO.render("this is [green]green[/]"));
    }

    @Test
    void emitsForegroundColorWhenEnabled() {
        IO.setStylingEnabled(true);
        assertEquals("[0;32mgreen[0m", IO.render("[green]green[/]"));
    }

    @Test
    void emitsBackgroundColor() {
        IO.setStylingEnabled(true);
        assertEquals("[0;41mtext[0m", IO.render("[bg-red]text[/]"));
    }

    @Test
    void emitsTextStyle() {
        IO.setStylingEnabled(true);
        assertEquals("[0;1mtext[0m", IO.render("[bold]text[/]"));
    }

    @Test
    void combinesNestedTags() {
        IO.setStylingEnabled(true);
        assertEquals(
                "[0;1m[0;1;32m[0;1;32;41mtext[0;1;32m[0;1m[0m",
                IO.render("[bold][green][bg-red]text[/][/][/]"));
    }

    @Test
    void restoresEnclosingStateOnNestedClose() {
        IO.setStylingEnabled(true);
        assertEquals(
                "[0;32ma[0;32;31mb[0;32mc[0m",
                IO.render("[green]a[red]b[/]c[/]"));
    }

    @Test
    void unclosedTagThrows() {
        IO.setStylingEnabled(true);
        assertThrows(IllegalArgumentException.class, () -> IO.render("[green]never closed"));
    }

    @Test
    void unmatchedClosingTagThrows() {
        IO.setStylingEnabled(true);
        assertThrows(IllegalArgumentException.class, () -> IO.render("no open[/]"));
    }

    @Test
    void unknownColorTokenThrows() {
        IO.setStylingEnabled(true);
        assertThrows(IllegalArgumentException.class, () -> IO.render("[chartreuse]x[/]"));
    }

    @Test
    void unknownBackgroundTokenThrows() {
        IO.setStylingEnabled(true);
        assertThrows(IllegalArgumentException.class, () -> IO.render("[bg-chartreuse]x[/]"));
    }

    @Test
    void placeholdersResolveBeforeTagsSoArgsCannotInjectTags() {
        IO.setStylingEnabled(true);
        // The arg's own [red] text is literal — only tags written in the template are live.
        assertEquals("plain [red]x[/] text", IO.render("plain {} text", "[red]x[/]"));
    }

    @Test
    void printAndPrintlnWriteToConfiguredStream() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        IO.setStylingEnabled(false);
        IO.use(new PrintStream(buffer));

        IO.print("Hello {}", "Ada");
        IO.println("!");

        assertEquals("Hello Ada!\n", buffer.toString());
    }

    @Test
    void useIgnoresNull() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        IO.setStylingEnabled(false);
        IO.use(new PrintStream(buffer));
        IO.use(null);

        IO.print("still here");

        assertEquals("still here", buffer.toString());
    }
}

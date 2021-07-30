package co.arago.hiro.client.util;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

/**
 * A pretty printer for jackson that indents arrays as well.
 *
 * @author duke
 */
public class PrettyPrinterFactory {

    /**
     * Setup indentation for all constructors.
     */
    public static PrettyPrinter createDefault() {
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        DefaultPrettyPrinter.Indenter arrayIndent = new DefaultIndenter("  ", DefaultIndenter.SYS_LF);
        prettyPrinter.indentArraysWith(arrayIndent);
        prettyPrinter.indentObjectsWith(arrayIndent);
        return prettyPrinter;
    }
}

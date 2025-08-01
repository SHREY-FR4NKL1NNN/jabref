package org.jabref.logic.bst;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.cleanup.ConvertToBibtexCleanup;
import org.jabref.logic.formatter.bibtexfields.RemoveNewlinesFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.layout.format.RemoveLatexCommandsFormatter;
import org.jabref.logic.layout.format.RemoveTilde;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BstPreviewLayout implements PreviewLayout {

    private static final Logger LOGGER = LoggerFactory.getLogger(BstPreviewLayout.class);

    private final String name;
    private String source;
    private BstVM bstVM;
    private String error;

    public BstPreviewLayout(Path path) {
        try {
            this.source = String.join("\n", Files.readAllLines(path));
        } catch (IOException e) {
            LOGGER.error("Error reading file", e);
            this.source = "";
        }

        name = path.getFileName().toString();
        if (!Files.exists(path)) {
            LOGGER.error("File {} not found", path.toAbsolutePath());
            error = Localization.lang("Error opening file '%0'", path.toString());
            return;
        }
        try {
            bstVM = new BstVM(path);
        } catch (IOException e) {
            LOGGER.error("Could not read {}.", path.toAbsolutePath(), e);
            error = Localization.lang("Error opening file '%0'", path.toString());
        }
    }

    @Override
    public String generatePreview(BibEntry originalEntry, BibDatabaseContext databaseContext) {
        if (error != null) {
            return error;
        }
        // ensure that the entry is of BibTeX format (and do not modify the original entry)
        BibEntry entry = new BibEntry(originalEntry);
        new ConvertToBibtexCleanup().cleanup(entry);
        String result = bstVM.render(List.of(entry));
        // Remove all comments
        result = result.replaceAll("%.*", "");
        // Remove all LaTeX comments
        // The RemoveLatexCommandsFormatter keeps the words inside latex environments. Therefore, we remove them manually
        result = result.replace("\\begin{thebibliography}{1}", "");
        result = result.replace("\\end{thebibliography}", "");
        // The RemoveLatexCommandsFormatter keeps the word inside the latex command, but we want to remove that completely
        result = result.replaceAll("\\\\bibitem[{].*[}]", "");
        // We want to replace \newblock by a space instead of completely removing it
        result = result.replace("\\newblock", " ");
        // remove all latex commands statements - assumption: command in a separate line
        result = result.replaceAll("(?m)^\\\\.*$", "");
        // remove some IEEEtran.bst output (resulting from a multiline \providecommand)
        result = result.replace("#2}}", "");
        // Have quotes right - and more
        result = new LatexToUnicodeFormatter().format(result);
        result = result.replace("``", "\"");
        result = result.replace("''", "\"");
        // Final cleanup
        result = new RemoveNewlinesFormatter().format(result);
        result = new RemoveLatexCommandsFormatter().format(result);
        result = new RemoveTilde().format(result);
        result = result.trim().replaceAll("  +", " ");
        return result;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getText() {
        return source;
    }

    /**
     * Checks if the given style file is a BST file by checking the extension
     */
    public static boolean isBstStyleFile(String styleFile) {
        return StandardFileType.BST.getExtensions().stream().anyMatch(styleFile::endsWith);
    }
}

package org.jabref.gui.collab.entrychange;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeResolver;
import org.jabref.gui.mergeentries.threewaymerge.EntriesMergeResult;
import org.jabref.gui.mergeentries.threewaymerge.MergeEntriesDialog;
import org.jabref.gui.mergeentries.threewaymerge.ShowDiffConfig;
import org.jabref.gui.mergeentries.threewaymerge.diffhighlighter.DiffHighlighter.BasicDiffMethod;
import org.jabref.gui.mergeentries.threewaymerge.toolbar.ThreeWayMergeToolbar;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

public final class EntryChangeResolver extends DatabaseChangeResolver {
    private final EntryChange entryChange;
    private final BibDatabaseContext databaseContext;

    private final GuiPreferences preferences;

    public EntryChangeResolver(EntryChange entryChange, DialogService dialogService, BibDatabaseContext databaseContext, GuiPreferences preferences) {
        super(dialogService);
        this.entryChange = entryChange;
        this.databaseContext = databaseContext;
        this.preferences = preferences;
    }

    @Override
    public Optional<DatabaseChange> askUserToResolveChange() {
        MergeEntriesDialog mergeEntriesDialog = new MergeEntriesDialog(entryChange.getOldEntry(), entryChange.getNewEntry(), preferences);
        mergeEntriesDialog.setLeftHeaderText(Localization.lang("In JabRef"));
        mergeEntriesDialog.setRightHeaderText(Localization.lang("On disk"));
        mergeEntriesDialog.configureDiff(new ShowDiffConfig(ThreeWayMergeToolbar.DiffView.SPLIT, BasicDiffMethod.WORDS));

        return dialogService.showCustomDialogAndWait(mergeEntriesDialog)
                            .map(this::mapMergeResultToExternalChange);
    }

    private EntryChange mapMergeResultToExternalChange(EntriesMergeResult entriesMergeResult) {
        return new EntryChange(
                entryChange.getOldEntry(),
                entriesMergeResult.mergedEntry(),
                databaseContext
        );
    }
}

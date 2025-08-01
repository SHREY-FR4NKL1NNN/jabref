package org.jabref.model.entry;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.util.URLUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

@Execution(CONCURRENT)
class BibEntryTest {
    private BibEntry entry = new BibEntry();

    @Test
    void defaultConstructor() {
        assertEquals(StandardEntryType.Misc, entry.getType());
        assertNotNull(entry.getId());
        assertFalse(entry.getField(StandardField.AUTHOR).isPresent());
    }

    @Test
    void settingTypeToNullThrowsException() {
        assertThrows(NullPointerException.class, () -> entry.setType(null));
    }

    @Test
    void setNullFieldThrowsNPE() {
        assertThrows(NullPointerException.class, () -> entry.setField(null));
    }

    @Test
    void getFieldIsCaseInsensitive() {
        entry.setField(new UnknownField("TeSt"), "value");
        assertEquals(Optional.of("value"), entry.getField(new UnknownField("tEsT")));
    }

    @Test
    void getFieldWorksWithBibFieldAsWell() {
        entry.setField(StandardField.AUTHOR, "value");
        assertEquals(Optional.of("value"), entry.getField(new BibField(StandardField.AUTHOR, FieldPriority.IMPORTANT).field()));
    }

    @Test
    void newBibEntryIsUnchanged() {
        assertFalse(entry.hasChanged());
    }

    @Test
    void setFieldLeadsToAChangedEntry() {
        entry.setField(StandardField.AUTHOR, "value");
        assertTrue(entry.hasChanged());
    }

    @Test
    void setFieldWorksWithBibFieldAsWell() {
        entry.setField(new BibField(StandardField.AUTHOR, FieldPriority.IMPORTANT).field(), "value");
        assertEquals(Optional.of("value"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void clonedBibEntryHasUniqueID() {
        BibEntry entryClone = new BibEntry(entry);
        assertNotEquals(entry.getId(), entryClone.getId());
    }

    @Test
    void clonedBibEntryWithMiscTypeHasOriginalChangedFlag() {
        BibEntry entryClone = new BibEntry(entry);
        assertFalse(entryClone.hasChanged());
    }

    @Test
    void clonedBibEntryWithBookTypeAndOneFieldHasOriginalChangedFlag() {
        entry = new BibEntry(StandardEntryType.Book).withField(StandardField.AUTHOR, "value");
        BibEntry entryClone = new BibEntry(entry);
        assertFalse(entryClone.hasChanged());
    }

    @Test
    void setAndGetAreConsistentForMonth() {
        entry.setField(StandardField.MONTH, "may");
        assertEquals(Optional.of("may"), entry.getField(StandardField.MONTH));
    }

    @Test
    void setAndGetAreConsistentForCapitalizedMonth() {
        entry.setField(StandardField.MONTH, "May");
        assertEquals(Optional.of("May"), entry.getField(StandardField.MONTH));
    }

    @Test
    void setAndGetAreConsistentForMonthString() {
        entry.setField(StandardField.MONTH, "#may#");
        assertEquals(Optional.of("#may#"), entry.getField(StandardField.MONTH));
    }

    @Test
    void monthCorrectlyReturnedForMonth() {
        entry.setField(StandardField.MONTH, "may");
        assertEquals(Optional.of(Month.MAY), entry.getMonth());
    }

    @Test
    void monthCorrectlyReturnedForCapitalizedMonth() {
        entry.setField(StandardField.MONTH, "May");
        assertEquals(Optional.of(Month.MAY), entry.getMonth());
    }

    @Test
    void monthCorrectlyReturnedForMonthString() {
        entry.setField(StandardField.MONTH, "#may#");
        assertEquals(Optional.of(Month.MAY), entry.getMonth());
    }

    @Test
    void monthCorrectlyReturnedForMonthMay() {
        entry.setMonth(Month.MAY);
        assertEquals(Optional.of(Month.MAY), entry.getMonth());
    }

    @Test
    void monthFieldCorrectlyReturnedForMonthMay() {
        entry.setMonth(Month.MAY);
        assertEquals(Optional.of("#may#"), entry.getField(StandardField.MONTH));
    }

    @Test
    void getFieldOrAliasDateWithYearNumericalMonthString() {
        entry.setField(StandardField.YEAR, "2003");
        entry.setField(StandardField.MONTH, "3");
        assertEquals(Optional.of("2003-03"), entry.getFieldOrAlias(StandardField.DATE));
    }

    @Test
    void getFieldOrAliasDateWithYearAbbreviatedMonth() {
        entry.setField(StandardField.YEAR, "2003");
        entry.setField(StandardField.MONTH, "#mar#");
        assertEquals(Optional.of("2003-03"), entry.getFieldOrAlias(StandardField.DATE));
    }

    @Test
    void getFieldOrAliasDateWithYearAbbreviatedMonthString() {
        entry.setField(StandardField.YEAR, "2003");
        entry.setField(StandardField.MONTH, "mar");
        assertEquals(Optional.of("2003-03"), entry.getFieldOrAlias(StandardField.DATE));
    }

    @Test
    void getFieldOrAliasDateWithOnlyYear() {
        entry.setField(StandardField.YEAR, "2003");
        assertEquals(Optional.of("2003"), entry.getFieldOrAlias(StandardField.DATE));
    }

    @Test
    void getFieldOrAliasYearWithDateYYYY() {
        entry.setField(StandardField.DATE, "2003");
        assertEquals(Optional.of("2003"), entry.getFieldOrAlias(StandardField.YEAR));
    }

    @Test
    void getFieldOrAliasYearWithDateYYYYMM() {
        entry.setField(StandardField.DATE, "2003-03");
        assertEquals(Optional.of("2003"), entry.getFieldOrAlias(StandardField.YEAR));
    }

    @Test
    void getFieldOrAliasYearWithDateYYYYMMDD() {
        entry.setField(StandardField.DATE, "2003-03-30");
        assertEquals(Optional.of("2003"), entry.getFieldOrAlias(StandardField.YEAR));
    }

    @Test
    void getFieldOrAliasMonthWithDateYYYYReturnsNull() {
        entry.setField(StandardField.DATE, "2003");
        assertEquals(Optional.empty(), entry.getFieldOrAlias(StandardField.MONTH));
    }

    @Test
    void getFieldOrAliasMonthWithDateYYYYMM() {
        entry.setField(StandardField.DATE, "2003-03");
        assertEquals(Optional.of("#mar#"), entry.getFieldOrAlias(StandardField.MONTH));
    }

    @Test
    void getFieldOrAliasMonthWithDateYYYYMMDD() {
        entry.setField(StandardField.DATE, "2003-03-30");
        assertEquals(Optional.of("#mar#"), entry.getFieldOrAlias(StandardField.MONTH));
    }

    @Test
    void getFieldOrAliasLatexFreeAlreadyFreeValueIsUnchanged() {
        entry.setField(StandardField.TITLE, "A Title Without any LaTeX commands");
        assertEquals(Optional.of("A Title Without any LaTeX commands"), entry.getFieldOrAliasLatexFree(StandardField.TITLE));
    }

    @Test
    void getFieldOrAliasLatexFreeAlreadyFreeAliasValueIsUnchanged() {
        entry.setField(StandardField.JOURNAL, "A Title Without any LaTeX commands");
        assertEquals(Optional.of("A Title Without any LaTeX commands"), entry.getFieldOrAliasLatexFree(StandardField.JOURNALTITLE));
    }

    @Test
    void getFieldOrAliasLatexFreeBracesAreRemoved() {
        entry.setField(StandardField.TITLE, "{A Title with some {B}ra{C}es}");
        assertEquals(Optional.of("A Title with some BraCes"), entry.getFieldOrAliasLatexFree(StandardField.TITLE));
    }

    @Test
    void getFieldOrAliasLatexFreeBracesAreRemovedFromAlias() {
        entry.setField(StandardField.JOURNAL, "{A Title with some {B}ra{C}es}");
        assertEquals(Optional.of("A Title with some BraCes"), entry.getFieldOrAliasLatexFree(StandardField.JOURNALTITLE));
    }

    @Test
    void getFieldOrAliasLatexFreeComplexConversionInAlias() {
        entry.setField(StandardField.JOURNAL, "A 32~{mA} {$\\Sigma\\Delta$}-modulator");
        assertEquals(Optional.of("A 32 mA ΣΔ-modulator"), entry.getFieldOrAliasLatexFree(StandardField.JOURNALTITLE));
    }

    @Test
    void getAndAddToLinkedFileList() {
        List<LinkedFile> files = entry.getFiles();
        files.add(new LinkedFile("", Path.of(""), ""));
        entry.setFiles(files);
        assertEquals(List.of(new LinkedFile("", Path.of(""), "")), entry.getFiles());
    }

    @Test
    void replaceOfLinkWorks() throws MalformedURLException {
        List<LinkedFile> files = new ArrayList<>();
        String urlAsString = "https://www.example.org/file.pdf";
        files.add(new LinkedFile(URLUtil.create(urlAsString), ""));
        entry.setFiles(files);

        LinkedFile linkedFile = new LinkedFile("", Path.of("file.pdf", ""), "");
        entry.replaceDownloadedFile(urlAsString, linkedFile);
        assertEquals(List.of(linkedFile), entry.getFiles());
    }

    @Test
    void getEmptyKeywords() {
        KeywordList actual = entry.getKeywords(',');

        assertEquals(new KeywordList(), actual);
    }

    @Test
    void getSingleKeywords() {
        entry.addKeyword("kw", ',');
        KeywordList actual = entry.getKeywords(',');

        assertEquals(new KeywordList(new Keyword("kw")), actual);
    }

    @Test
    void settingCiteKeyLeadsToCorrectCiteKey() {
        assertFalse(entry.hasCitationKey());
        entry.setCitationKey("Einstein1931");
        assertEquals(Optional.of("Einstein1931"), entry.getCitationKey());
    }

    @Test
    void settingCiteKeyLeadsToHasCiteKy() {
        assertFalse(entry.hasCitationKey());
        entry.setCitationKey("Einstein1931");
        assertTrue(entry.hasCitationKey());
    }

    @Test
    void clearFieldWorksForAuthor() {
        entry.setField(StandardField.AUTHOR, "Albert Einstein");
        entry.clearField(StandardField.AUTHOR);
        assertEquals(Optional.empty(), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void setFieldWorksForAuthor() {
        entry.setField(StandardField.AUTHOR, "Albert Einstein");
        assertEquals(Optional.of("Albert Einstein"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void allFieldsPresentDefault() {
        BibEntry e = new BibEntry(StandardEntryType.Article);
        e.setField(StandardField.AUTHOR, "abc");
        e.setField(StandardField.TITLE, "abc");
        e.setField(StandardField.JOURNAL, "abc");

        List<OrFields> requiredFields = new ArrayList<>();
        requiredFields.add(new OrFields(StandardField.AUTHOR));
        requiredFields.add(new OrFields(StandardField.TITLE));
        assertTrue(e.allFieldsPresent(requiredFields, null));

        requiredFields.add(new OrFields(StandardField.YEAR));
        assertFalse(e.allFieldsPresent(requiredFields, null));
    }

    @Test
    void allFieldsPresentOr() {
        BibEntry e = new BibEntry(StandardEntryType.Article);
        e.setField(StandardField.AUTHOR, "abc");
        e.setField(StandardField.TITLE, "abc");
        e.setField(StandardField.JOURNAL, "abc");

        List<OrFields> requiredFields = new ArrayList<>();
        requiredFields.add(new OrFields(StandardField.JOURNAL, StandardField.YEAR));
        assertTrue(e.allFieldsPresent(requiredFields, null));

        requiredFields.add(new OrFields(StandardField.YEAR, StandardField.ADDRESS));
        assertFalse(e.allFieldsPresent(requiredFields, null));
    }

    @Test
    void isNullCiteKeyThrowsNPE() {
        BibEntry e = new BibEntry(StandardEntryType.Article);
        assertThrows(NullPointerException.class, () -> e.setCitationKey(null));
    }

    @Test
    void isEmptyCiteKey() {
        BibEntry e = new BibEntry(StandardEntryType.Article);
        assertFalse(e.hasCitationKey());

        e.setCitationKey("");
        assertFalse(e.hasCitationKey());

        e.setCitationKey("key");
        assertTrue(e.hasCitationKey());

        e.clearField(InternalField.KEY_FIELD);
        assertFalse(e.hasCitationKey());
    }

    @Test
    void identicObjectsareEqual() {
        BibEntry otherEntry = entry;
        assertEquals(entry, otherEntry);
    }

    @Test
    void compareToNullObjectIsFalse() {
        assertNotEquals(null, entry);
    }

    @Test
    void compareToDifferentClassIsFalse() {
        assertNotEquals(new Object(), entry);
    }

    @Test
    void compareIsTrueWhenIdAndFieldsAreEqual() {
        entry.setId("1");
        entry.setField(new UnknownField("key"), "value");
        BibEntry otherEntry = new BibEntry();
        otherEntry.setId("1");
        assertNotEquals(entry, otherEntry);
        otherEntry.setField(new UnknownField("key"), "value");
        assertEquals(entry, otherEntry);
    }

    @Test
    void addNullKeywordThrowsNPE() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        assertThrows(NullPointerException.class, () -> entry.addKeyword((Keyword) null, ','));
    }

    @Test
    void putNullKeywordListThrowsNPE() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        assertThrows(NullPointerException.class, () -> entry.putKeywords((KeywordList) null, ','));
    }

    @Test
    void putNullKeywordSeparatorThrowsNPE() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        assertThrows(NullPointerException.class, () -> entry.putKeywords(Arrays.asList("A", "B"), null));
    }

    @Test
    void getSeparatedKeywordsAreCorrect() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        assertEquals(new KeywordList("Foo", "Bar"), entry.getKeywords(','));
    }

    @Test
    void addKeywordIsCorrect() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        entry.addKeyword("FooBar", ',');
        assertEquals(new KeywordList("Foo", "Bar", "FooBar"), entry.getKeywords(','));
    }

    @Test
    void addKeywordHasChanged() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        entry.addKeyword("FooBar", ',');
        assertTrue(entry.hasChanged());
    }

    @Test
    void addKeywordTwiceYiedsOnlyOne() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        entry.addKeyword("FooBar", ',');
        entry.addKeyword("FooBar", ',');
        assertEquals(new KeywordList("Foo", "Bar", "FooBar"), entry.getKeywords(','));
    }

    @Test
    void addKeywordIsCaseSensitive() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        entry.addKeyword("FOO", ',');
        assertEquals(new KeywordList("Foo", "Bar", "FOO"), entry.getKeywords(','));
    }

    @Test
    void addKeywordWithDifferentCapitalizationChanges() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        entry.addKeyword("FOO", ',');
        assertTrue(entry.hasChanged());
    }

    @Test
    void addKeywordEmptyKeywordIsNotAdded() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        entry.addKeyword("", ',');
        assertEquals(new KeywordList("Foo", "Bar"), entry.getKeywords(','));
    }

    @Test
    void addKeywordEmptyKeywordNotChanged() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        entry.setChanged(false);
        entry.addKeyword("", ',');
        assertFalse(entry.hasChanged());
    }

    @Test
    void texNewBibEntryHasNoKeywords() {
        assertTrue(entry.getKeywords(',').isEmpty());
    }

    @Test
    void texNewBibEntryHasNoKeywordsEvenAfterAddingEmptyKeyword() {
        entry.addKeyword("", ',');
        assertTrue(entry.getKeywords(',').isEmpty());
    }

    @Test
    void texNewBibEntryAfterAddingEmptyKeywordNotChanged() {
        entry.addKeyword("", ',');
        assertFalse(entry.hasChanged());
    }

    @Test
    void addKeywordsWorksAsExpected() {
        entry.addKeywords(Arrays.asList("Foo", "Bar"), ',');
        assertEquals(new KeywordList("Foo", "Bar"), entry.getKeywords(','));
    }

    @Test
    void putKeywordsOverwritesOldKeywords() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        entry.putKeywords(Arrays.asList("Yin", "Yang"), ',');
        assertEquals(new KeywordList("Yin", "Yang"), entry.getKeywords(','));
    }

    @Test
    void putKeywordsHasChanged() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        entry.putKeywords(Arrays.asList("Yin", "Yang"), ',');
        assertTrue(entry.hasChanged());
    }

    @Test
    void putKeywordsPutEmpyListErasesPreviousKeywords() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        entry.putKeywords(List.of(), ',');
        assertTrue(entry.getKeywords(',').isEmpty());
    }

    @Test
    void putKeywordsPutEmpyListHasChanged() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        entry.putKeywords(List.of(), ',');
        assertTrue(entry.hasChanged());
    }

    @Test
    void putKeywordsPutEmpyListToEmptyBibentry() {
        entry.putKeywords(List.of(), ',');
        assertTrue(entry.getKeywords(',').isEmpty());
    }

    @Test
    void putKeywordsPutEmpyListToEmptyBibentryNotChanged() {
        entry.putKeywords(List.of(), ',');
        assertFalse(entry.hasChanged());
    }

    @Test
    void putKeywordsToEmptyReturnsNoChange() {
        Optional<FieldChange> change = entry.putKeywords(List.of(), ',');
        assertEquals(Optional.empty(), change);
    }

    @Test
    void clearKeywordsReturnsChange() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        Optional<FieldChange> change = entry.putKeywords(List.of(), ',');
        assertEquals(Optional.of(new FieldChange(entry, StandardField.KEYWORDS, "Foo, Bar", null)), change);
    }

    @Test
    void changeKeywordsReturnsChange() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        Optional<FieldChange> change = entry.putKeywords(Arrays.asList("Test", "FooTest"), ',');
        assertEquals(Optional.of(new FieldChange(entry, StandardField.KEYWORDS, "Foo, Bar", "Test, FooTest")),
                change);
    }

    @Test
    void putKeywordsToSameReturnsNoChange() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        Optional<FieldChange> change = entry.putKeywords(Arrays.asList("Foo", "Bar"), ',');
        assertEquals(Optional.empty(), change);
    }

    @Test
    void getKeywordsReturnsParsedKeywordListFromKeywordsField() {
        entry.setField(StandardField.KEYWORDS, "w1, w2a w2b, w3");
        assertEquals(new KeywordList("w1", "w2a w2b", "w3"), entry.getKeywords(','));
    }

    @Test
    void removeKeywordsOnEntryWithoutKeywordsDoesNothing() {
        Optional<FieldChange> change = entry.removeKeywords(SpecialField.RANKING.getKeyWords(), ',');
        assertEquals(Optional.empty(), change);
    }

    @Test
    void removeKeywordsWithEmptyListDoesNothing() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        entry.putKeywords(Arrays.asList("kw1", "kw2"), ',');
        Optional<FieldChange> change = entry.removeKeywords(new KeywordList(), ',');
        assertEquals(Optional.empty(), change);
    }

    @Test
    void removeKeywordsWithNonExistingKeywordsDoesNothing() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        entry.putKeywords(Arrays.asList("kw1", "kw2"), ',');
        Optional<FieldChange> change = entry.removeKeywords(KeywordList.parse("kw3, kw4", ','), ',');
        assertEquals(Optional.empty(), change);
        assertEquals(Sets.newHashSet("kw1", "kw2"), entry.getKeywords(',').toStringList());
    }

    @Test
    void removeKeywordsWithExistingKeywordsRemovesThem() {
        entry.setField(StandardField.KEYWORDS, "Foo, Bar");
        entry.putKeywords(Arrays.asList("kw1", "kw2", "kw3"), ',');
        Optional<FieldChange> change = entry.removeKeywords(KeywordList.parse("kw1, kw2", ','), ',');
        assertTrue(change.isPresent());
        assertEquals(KeywordList.parse("kw3", ','), entry.getKeywords(','));
    }

    @Test
    void keywordListCorrectlyConstructedForThreeKeywords() {
        entry.addKeyword("kw", ',');
        entry.addKeyword("kw2", ',');
        entry.addKeyword("kw3", ',');
        KeywordList actual = entry.getKeywords(',');
        assertEquals(new KeywordList(new Keyword("kw"), new Keyword("kw2"), new Keyword("kw3")), actual);
    }

    @Test
    void getEmptyResolvedKeywords() {
        BibDatabase database = new BibDatabase();
        entry.setField(StandardField.CROSSREF, "entry2");
        database.insertEntry(entry);

        BibEntry entry2 = new BibEntry();
        entry2.setCitationKey("entry2");
        database.insertEntry(entry2);

        KeywordList actual = entry.getResolvedKeywords(',', database);

        assertEquals(new KeywordList(), actual);
    }

    @Test
    void getSingleResolvedKeywords() {
        BibDatabase database = new BibDatabase();
        entry.setField(StandardField.CROSSREF, "entry2");

        BibEntry entry2 = new BibEntry();
        entry2.setCitationKey("entry2");
        entry2.addKeyword("kw", ',');

        database.insertEntry(entry2);
        database.insertEntry(entry);

        KeywordList actual = entry.getResolvedKeywords(',', database);

        assertEquals(new KeywordList(new Keyword("kw")), actual);
    }

    @Test
    void getResolvedKeywords() {
        BibDatabase database = new BibDatabase();
        entry.setField(StandardField.CROSSREF, "entry2");

        BibEntry entry2 = new BibEntry();
        entry2.setCitationKey("entry2");
        entry2.addKeyword("kw", ',');
        entry2.addKeyword("kw2", ',');
        entry2.addKeyword("kw3", ',');

        database.insertEntry(entry2);
        database.insertEntry(entry);

        KeywordList actual = entry.getResolvedKeywords(',', database);

        assertEquals(new KeywordList(new Keyword("kw"), new Keyword("kw2"), new Keyword("kw3")), actual);
    }

    @Test
    void settingTitleFieldsLeadsToChangeFlagged() {
        entry.setField(StandardField.AUTHOR, "value");
        assertTrue(entry.hasChanged());
    }

    @Test
    void builderReturnsABibEntryNotChangedFlagged() {
        entry = new BibEntry().withField(StandardField.AUTHOR, "value");
        assertFalse(entry.hasChanged());
    }

    @Test
    void mergeEntriesWithNoOverlap() {
        BibEntry expected = new BibEntry()
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.EPRINT, "1234.56789")
                .withField(StandardField.DATE, "1970-01-01");

        BibEntry copyEntry = new BibEntry(entry);
        BibEntry otherEntry = new BibEntry();

        copyEntry.setField(Map.of(
                StandardField.AUTHOR, "Test Author",
                StandardField.TITLE, "Test Title"));

        otherEntry.setField(Map.of(
                StandardField.EPRINT, "1234.56789",
                StandardField.DATE, "1970-01-01"
        ));

        copyEntry.mergeWith(otherEntry);
        assertEquals(expected.getFields(), copyEntry.getFields());
    }

    @Test
    void mergeEntriesWithOverlap() {
        BibEntry expected = new BibEntry()
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DATE, "1970-01-01");

        BibEntry copyEntry = new BibEntry(entry);
        BibEntry otherEntry = new BibEntry();

        copyEntry.setField(Map.of(
                StandardField.AUTHOR, "Test Author",
                StandardField.TITLE, "Test Title"));

        otherEntry.setField(Map.of(
                StandardField.AUTHOR, "Another Test Author",
                StandardField.DATE, "1970-01-01"
        ));

        copyEntry.mergeWith(otherEntry);
        assertEquals(expected.getFields(), copyEntry.getFields());
    }

    @Test
    void mergeEntriesWithNoOverlapAndNonExistingPriorityFields() {
        BibEntry expected = new BibEntry()
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.EPRINT, "1234.56789")
                .withField(StandardField.DATE, "1970-01-01");

        BibEntry copyEntry = new BibEntry(entry);
        BibEntry otherEntry = new BibEntry();

        copyEntry.setField(Map.of(
                StandardField.AUTHOR, "Test Author",
                StandardField.TITLE, "Test Title"));

        otherEntry.setField(Map.of(
                StandardField.EPRINT, "1234.56789",
                StandardField.DATE, "1970-01-01"
        ));

        Set<Field> otherPrioritizedFields = Set.of(StandardField.VOLUME, StandardField.KEYWORDS);

        copyEntry.mergeWith(otherEntry, otherPrioritizedFields);
        assertEquals(expected.getFields(), copyEntry.getFields());
    }

    @Test
    void mergeEntriesWithNoOverlapAndExistingPriorityFields() {
        BibEntry expected = new BibEntry()
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.EPRINT, "1234.56789")
                .withField(StandardField.DATE, "1970-01-01");

        BibEntry copyEntry = new BibEntry(entry);
        BibEntry otherEntry = new BibEntry();

        copyEntry.setField(Map.of(
                StandardField.AUTHOR, "Test Author",
                StandardField.TITLE, "Test Title"));

        otherEntry.setField(Map.of(
                StandardField.EPRINT, "1234.56789",
                StandardField.DATE, "1970-01-01"
        ));

        Set<Field> otherPrioritizedFields = Set.of(StandardField.AUTHOR, StandardField.EPRINT);

        copyEntry.mergeWith(otherEntry, otherPrioritizedFields);
        assertEquals(expected.getFields(), copyEntry.getFields());
    }

    @Test
    void mergeEntriesWithOverlapAndPriorityGivenToNonOverlappingField() {
        BibEntry expected = new BibEntry()
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DATE, "1970-01-01");

        BibEntry copyEntry = new BibEntry(entry);
        BibEntry otherEntry = new BibEntry();

        copyEntry.setField(Map.of(
                StandardField.AUTHOR, "Test Author",
                StandardField.TITLE, "Test Title"));

        otherEntry.setField(Map.of(
                StandardField.AUTHOR, "Another Test Author",
                StandardField.DATE, "1970-01-01"
        ));

        Set<Field> otherPrioritizedFields = Set.of(StandardField.TITLE, StandardField.DATE);

        copyEntry.mergeWith(otherEntry, otherPrioritizedFields);
        assertEquals(expected.getFields(), copyEntry.getFields());
    }

    @Test
    void mergeEntriesWithOverlapAndPriorityGivenToOverlappingField() {
        BibEntry expected = new BibEntry()
                .withField(StandardField.AUTHOR, "Another Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DATE, "1970-01-01");

        BibEntry copyEntry = new BibEntry(entry);
        BibEntry otherEntry = new BibEntry();

        copyEntry.setField(Map.of(
                StandardField.AUTHOR, "Test Author",
                StandardField.TITLE, "Test Title"));

        otherEntry.setField(Map.of(
                StandardField.AUTHOR, "Another Test Author",
                StandardField.DATE, "1970-01-01"
        ));

        Set<Field> otherPrioritizedFields = Set.of(StandardField.AUTHOR, StandardField.DATE);

        copyEntry.mergeWith(otherEntry, otherPrioritizedFields);
        assertEquals(expected.getFields(), copyEntry.getFields());
    }

    @ParameterizedTest
    @CsvSource({
            "John Doe, Some Title, 2023, Doe: \"Some Title\" (2023)",
            "Jane Smith and Tom Brown, Another Research, 2022, Smith and Brown: \"Another Research\" (2022)",
            ", Some Title, , N/A: \"Some Title\" (N/A)",
            "Single Author, , 2020, Author: \"N/A\" (2020)",
            "Test, Article, , Test: \"Article\" (N/A)",
            ", , , N/A: \"N/A\" (N/A)",
            "Author One, \\\"{O}lala Title, 2021, One: \"Ölala Title\" (2021)",
            "Another Author, A \\'{e}xample with \\\"u, 2019, Author: \"A éxample with ü\" (2019)",
            "Last One, Title with {B}races, 2024, One: \"Title with Braces\" (2024)"
    })
    void getAuthorTitleYearFormatted(String author, String title, String year, String expected) {
        entry.setField(StandardField.AUTHOR, author != null ? author : "");
        entry.setField(StandardField.TITLE, title != null ? title : "");
        entry.setField(StandardField.YEAR, year != null ? year : "");
        assertEquals(expected, entry.getAuthorTitleYear(0));
    }

    public static Stream<BibEntry> isEmpty() {
        return Stream.of(
                new BibEntry(),
                new BibEntry(StandardEntryType.Book),
                new BibEntry().withField(StandardField.OWNER, "test"),
                new BibEntry().withField(StandardField.CREATIONDATE, "test"),
                new BibEntry()
                        .withField(StandardField.OWNER, "test")
                        .withField(StandardField.CREATIONDATE, "test"),
                // source: https://github.com/JabRef/jabref/issues/8645
                new BibEntry()
                        .withField(StandardField.OWNER, "mlep")
                        .withField(StandardField.CREATIONDATE, "2022-04-05T10:41:54"));
    }

    @ParameterizedTest
    @MethodSource
    void isEmpty(BibEntry entry) {
        assertTrue(entry.isEmpty());
    }

    public static Stream<BibEntry> isNotEmpty() {
        return Stream.of(
                new BibEntry().withCitationKey("test"),
                new BibEntry().withField(StandardField.AUTHOR, "test")
        );
    }

    @ParameterizedTest
    @MethodSource
    void isNotEmpty(BibEntry entry) {
        assertFalse(entry.isEmpty());
    }
}

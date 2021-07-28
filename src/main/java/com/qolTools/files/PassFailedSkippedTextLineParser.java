package com.qolTools.files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

public class PassFailedSkippedTextLineParser {

    public static final String FAILED_TESTS_STRING = "THERE WERE  %s  FAILED TESTS.";
    public static final String SKIPPED_TESTS_STRING = "THERE WERE  %s  SKIPPED TESTS.";
    public static final String PASSED_TESTS_STRING = "THERE WERE  %s  PASSED TESTS.";
    public static final String TEST_CASE_TESTS_STRING = "THERE WERE  %s  TOTAL TEST CASE ITEMS.";

    public static void main(String[] args) {

        // Windows
        String copiedAbsPath = "C:\\Users\\cropr\\Downloads\\qol-Tools\\qol-Tools\\src\\main\\java\\com\\java\\qolTools\\files\\ExampleTestCaseOutputFile.txt";
        copiedAbsPath = copiedAbsPath.replaceAll("\\\\","/");

        String[] pathArray = copiedAbsPath.split("/");

        String allResultsAbsFilePath = copiedAbsPath.replaceAll("/", "//");

        String[] testRunFolder = Arrays.copyOf(pathArray, pathArray.length - 1); // outputs to same folder
        String testRunFolderPath = Arrays.toString(testRunFolder);

        testRunFolderPath = testRunFolderPath
                .replaceAll(", ", "//")
                .replaceAll("\\[", "")
                .replaceAll("\\]", "") + "//";

        Scanner allResultsFileReader;

        FileWriter passedTestWriter;
        FileWriter failedTestWriter;
        FileWriter skippedTestWriter;

        FileWriter testCaseTestWriter;

        try {
            String passedFileAbsPath = createPassedFile(testRunFolderPath);
            String failedFileAbsPath = createFailedFile(testRunFolderPath);
            String skippedFileAbsPath = createSkippedFile(testRunFolderPath);
            String tcOnlyFileAbsPath = createTestCaseFile(testRunFolderPath);

            passedTestWriter = new FileWriter(passedFileAbsPath);
            failedTestWriter = new FileWriter(failedFileAbsPath);
            skippedTestWriter = new FileWriter(skippedFileAbsPath);
            testCaseTestWriter = new FileWriter(tcOnlyFileAbsPath);


            File allResultsFile = new File(allResultsAbsFilePath);
            allResultsFileReader = new Scanner(allResultsFile);


            List<String> passedTestCollection = new ArrayList<>();
            List<String> failedTestCollection = new ArrayList<>();
            List<String> skippedTestCollection = new ArrayList<>();
            List<String> testCaseCollection = new ArrayList<>();


            String allResultsLine;
            while (allResultsFileReader.hasNextLine()) {

                allResultsLine = allResultsFileReader.nextLine();

                if (isFailedTest(allResultsLine)) {
                    failedTestCollection.add(allResultsLine);

                } else if (isSkippedTest(allResultsLine)) {
                    skippedTestCollection.add(allResultsLine);

                } else
                if(isPassedTest(allResultsLine)){
                    passedTestCollection.add(allResultsLine);
                }


                if (isTestCaseLine(allResultsLine)) {
                    testCaseCollection.add(allResultsLine);
                }
            }
            allResultsFileReader.close();


            writeTestsInAlphabeticalOrder(passedTestWriter, passedTestCollection);
            finalizeWritingFile(passedTestWriter, passedFileAbsPath, PASSED_TESTS_STRING);


            writeTestsInAlphabeticalOrder(failedTestWriter, failedTestCollection);
            finalizeWritingFile(failedTestWriter, failedFileAbsPath, FAILED_TESTS_STRING);


            writeTestsInAlphabeticalOrder(skippedTestWriter, skippedTestCollection);
            finalizeWritingFile(skippedTestWriter, skippedFileAbsPath, SKIPPED_TESTS_STRING);


            writeTestsInAlphabeticalOrder(testCaseTestWriter, testCaseCollection);
            finalizeWritingFile(testCaseTestWriter, tcOnlyFileAbsPath, TEST_CASE_TESTS_STRING);

        } catch (FileNotFoundException fnfException) {
            fnfException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static void writeTestsInAlphabeticalOrder(FileWriter fileWriter, List<String> unsortedTests) throws IOException {
        writeTestsInAlphabeticalOrder(fileWriter, unsortedTests, true);
    }

    private static void writeTestsInAlphabeticalOrder(FileWriter fileWriter, List<String> unsortedTests, boolean removeTcPrefix) throws IOException {

        if (removeTcPrefix) {
            unsortedTests = removeTcPrefixedItems(unsortedTests);
        } else {
            unsortedTests = (chopUpTcIdsFromMultipleOneSingleLinse(unsortedTests));
        }


        Collections.sort(unsortedTests);
        List<String> sortedList = unsortedTests;


        for (String testLine : sortedList) {
            fileWriter.write(testLine + "\n");
        }
        fileWriter.close();
    }

    private static List<String> removeTcPrefixedItems(List<String> unsortedTests) {

        Predicate<String> hasTcPrefixed = inputString -> {
            boolean prefixPresent = false;

            String[] parts = inputString.split(" ");

            if (parts.length > 1) prefixPresent = true;

            return prefixPresent;
        };


        List<String> tcPrefixRemovedList = new ArrayList<>();
        String[] parts;

        for (String reportLine : unsortedTests) {

            if (hasTcPrefixed.test(reportLine)) {
                parts = reportLine.split("");
                reportLine = parts[parts.length - 1];
            }

            tcPrefixRemovedList.add(reportLine);
        }

        return tcPrefixRemovedList;
    }

    private static List<String> chopUpTcIdsFromMultipleOneSingleLinse(List<String> linesOfTcItems) {

        Set testCasesForMethodSet = new HashSet();

        String[] partOfTcLine;

        for (String tcLine : linesOfTcItems) {

            partOfTcLine = tcLine.split(" ");

            String testNameWithStatus = partOfTcLine[partOfTcLine.length - 1];
            if (partOfTcLine.length > 2) {

                for (int i = 0; i < partOfTcLine.length - 2; i++) {
                    testCasesForMethodSet.add(partOfTcLine[i] + " " + testNameWithStatus);
                }

            } else {
                testCasesForMethodSet.add(tcLine);
            }
        }

        Set testCaseNumbersOnly = reduceToNumbersOnly(testCasesForMethodSet, true);
        return new ArrayList(testCaseNumbersOnly);
    }

    private static Set reduceToNumbersOnly(Set testCasesForMethodSet, boolean includeStatus) {

        Set uniqueTestCaseNumbersOnly = new HashSet();

        TreeSet<String> sortedTestCases = new TreeSet<>(testCasesForMethodSet);

        for (String testCaseNumberAndMethod : sortedTestCases) {

            String numberTextOnly = testCaseNumberAndMethod.split(" ")[0];
            String resultStatus = testCaseNumberAndMethod.split("\\|")[1];

            String tcNumberWithStatus = numberTextOnly + " " + resultStatus;

            uniqueTestCaseNumbersOnly.add(tcNumberWithStatus);
        }

        return uniqueTestCaseNumbersOnly;
    }

    private static void finalizeWritingFile(FileWriter fileWriter, String fileAbsPath, String outputString) throws IOException {

        fileWriter.close();

        String qtyTestOfThisType = new Integer(extractTestQtyInFile(fileAbsPath)).toString();
        String fileNameWithQty = fileAbsPath.replaceAll("%", "FOUND_" + qtyTestOfThisType);

        File newNameFile = new File(fileNameWithQty);
        File existingFile = new File(fileAbsPath);

        existingFile.renameTo(newNameFile);
    }

    private static int extractTestQtyInFile(String fileAbsPath) throws FileNotFoundException {

        File myFile = new File(fileAbsPath);
        Scanner fileReader = new Scanner(myFile);

        int testPresentQty = 0;

        while (fileReader.hasNextLine()) {

            fileReader.nextLine();
            testPresentQty++;
        }

        return testPresentQty;
    }

    private static boolean isTestCaseLine(String allResultsLine) {
        boolean isExpectedStatus = false;

        if (allResultsLine.length() > 0) {
            String[] array = allResultsLine.split("\\ ");

            if (array.length >= 2) {
                isExpectedStatus = true;
            }
        }
        return isExpectedStatus;
    }

    private static boolean isNonTestCaseLine(String allResultsLine, String expectedStatusText) {
        boolean isExpectedStatus = false;

        if (allResultsLine.length() > 0) {
            String[] array = allResultsLine.split("\\|");

            if ((array.length == 2) && array[1].trim().equalsIgnoreCase(expectedStatusText)) {
                isExpectedStatus = true;
            }
        }
        return isExpectedStatus;
    }

    private static boolean isPassedTest(String allResultsLine) {
        return isNonTestCaseLine(allResultsLine, "passed");
    }

    private static boolean isFailedTest(String allResultsLine) {
        return isNonTestCaseLine(allResultsLine, "failed");
    }

    private static boolean isSkippedTest(String allResultsLine) {
        return isNonTestCaseLine(allResultsLine, "skipped");
    }

    private static String createPassedFile(String testRunFolderPath) {
        return createFile(testRunFolderPath, "%_PASSED_Tests_");
    }

    private static String createFailedFile(String testRunFolderPath) {
        return createFile(testRunFolderPath, "%_FAILED_Tests_");
    }

    private static String createSkippedFile(String testRunFolderPath) {
        return createFile(testRunFolderPath, "%_SKIPPED_Tests_");
    }

    private static String createTestCaseFile(String testRunFolderPath) {
        return createFile(testRunFolderPath, "%_TC_ITEM_Tests_");
    }


    private static String createFile(String testRunFolderPath, String createdFileName) {
        String fileLocation = testRunFolderPath + createdFileName + ".txt";
        createdFileName(fileLocation);
        return fileLocation;
    }

    private static void createdFileName(String newFileAbsPath) {
        try {
            File newFile = new File(newFileAbsPath);
            if (newFile.createNewFile()) {
                System.out.println("File created: " + newFile.getName());
            }

        } catch (IOException ioException) {
            System.out.println("An error occurred");

        }
    }
}
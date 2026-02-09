package it.welf.alfresco.deleter.service;

import it.welf.alfresco.deleter.model.NodeInfo;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CsvService {

    public List<String> readNodeIds(File file) throws IOException {
        List<String> ids = new ArrayList<>();
        try (Reader reader = new FileReader(file);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                // Assuming first column is Node ID, or check header "Node ID"
                if (record.size() > 0) {
                    ids.add(record.get(0));
                }
            }
        }
        return ids;
    }

    public void writeReport(File file, List<NodeInfo> results) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath());
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("Node ID", "Name", "Status", "Message"))) {
            for (NodeInfo info : results) {
                printer.printRecord(info.getId(), info.getName(), info.getStatus(), info.getMessage());
            }
        }
    }
}

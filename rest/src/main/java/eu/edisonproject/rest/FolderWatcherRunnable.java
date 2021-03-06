/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.edisonproject.rest;

import static eu.edisonproject.rest.ECO2Controller.CSV_AVG_FILENAME;
import static eu.edisonproject.rest.ECO2Controller.CSV_FILE_NAME;
import static eu.edisonproject.rest.ECO2Controller.JSON_AVG_FILENAME;
import static eu.edisonproject.rest.ECO2Controller.JSON_FILE_NAME;
import static eu.edisonproject.rest.ECO2Controller.baseCategoryFolder;
import static eu.edisonproject.rest.ECO2Controller.courseAverageFolder;
import static eu.edisonproject.rest.ECO2Controller.courseClassisifcationFolder;
import static eu.edisonproject.rest.ECO2Controller.courseProfileFolder;
import static eu.edisonproject.rest.ECO2Controller.cvAverageFolder;
import static eu.edisonproject.rest.ECO2Controller.cvClassisifcationFolder;
import static eu.edisonproject.rest.ECO2Controller.cvProfileFolder;
import static eu.edisonproject.rest.ECO2Controller.jobAverageFolder;
import static eu.edisonproject.rest.ECO2Controller.jobClassisifcationFolder;
import static eu.edisonproject.rest.ECO2Controller.jobProfileFolder;
import static eu.edisonproject.rest.ECO2Controller.propertiesFile;

import eu.edisonproject.classification.main.BatchMain;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;

/**
 *
 * @author S. Koulouzis
 */
class FolderWatcherRunnable implements Runnable {

  private final String dir;

  public FolderWatcherRunnable(String dir) {
    this.dir = dir;
  }

  @Override
  public void run() {
    final Path path = FileSystems.getDefault().getPath(dir);
    try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
      final WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
      while (true) {
        final WatchKey wk = watchService.take();
        for (WatchEvent<?> event : wk.pollEvents()) {

          final Path changed = (Path) event.context();
          executeClassification(new File(dir + File.separator + changed));
        }
        // reset the key
        boolean valid = wk.reset();
        if (!valid) {
          Logger.getLogger(FolderWatcherRunnable.class.getName()).log(Level.WARNING, "Key has been unregisterede");
        }
      }
    } catch (IOException ex) {
      Logger.getLogger(FolderWatcherRunnable.class.getName()).log(Level.SEVERE, null, ex);
    } catch (InterruptedException ex) {
      Logger.getLogger(FolderWatcherRunnable.class.getName()).log(Level.SEVERE, null, ex);
    } catch (Exception ex) {
      Logger.getLogger(FolderWatcherRunnable.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private File executeClassification(File inputFolder) throws Exception {
    File txtFile = null;
    if (inputFolder.isFile()) {
      txtFile = inputFolder;
      inputFolder = inputFolder.getParentFile();
      Thread.sleep(100);
    }

    boolean isProfile = false;
    String partent = inputFolder.getParentFile().getAbsolutePath();
    if (partent.equals(jobProfileFolder.getAbsolutePath())
            || partent.equals(courseProfileFolder.getAbsolutePath())
            || partent.equals(cvProfileFolder.getAbsolutePath())) {
      isProfile = true;
    }

    if (!isProfile) {

      if (txtFile == null || txtFile.getName().endsWith(".txt")) {
        String[] args = new String[]{"-op", "c", "-i", inputFolder.getAbsolutePath(),
          "-o", inputFolder.getAbsolutePath(), "-c", baseCategoryFolder.getAbsolutePath(),
          "-p", propertiesFile.getAbsolutePath()};

        BatchMain.main(args);
        boolean calcAvg = false;
        if (inputFolder.getAbsolutePath().equals(jobAverageFolder.getAbsolutePath())
                || inputFolder.getAbsolutePath().equals(courseAverageFolder.getAbsolutePath())
                || inputFolder.getAbsolutePath().equals(cvAverageFolder.getAbsolutePath())) {
          calcAvg = true;
        }
        convertMRResultToCSV(new File(inputFolder.getAbsolutePath() + File.separator + "part-r-00000"),
                inputFolder.getAbsolutePath() + File.separator + ECO2Controller.CSV_FILE_NAME, calcAvg);

        convertCSVJsonFile(inputFolder.getAbsolutePath() + File.separator + CSV_AVG_FILENAME,
                inputFolder.getAbsolutePath() + File.separator + JSON_AVG_FILENAME);

        if (inputFolder.getParentFile().getAbsolutePath().equals(jobClassisifcationFolder.getAbsolutePath())) {
          for (File add : inputFolder.listFiles()) {
            if (add.getName().endsWith(".txt")) {
              FileUtils.copyFileToDirectory(add, jobAverageFolder);
            }
          }
        }

        if (inputFolder.getParentFile().getAbsolutePath().equals(courseClassisifcationFolder.getAbsolutePath())) {
          for (File add : inputFolder.listFiles()) {
            if (add.getName().endsWith(".txt")) {
              FileUtils.copyFileToDirectory(add, ECO2Controller.courseAverageFolder);
            }
          }
        }

        if (inputFolder.getParentFile().getAbsolutePath().equals(cvClassisifcationFolder.getAbsolutePath())) {
          for (File add : inputFolder.listFiles()) {
            if (add.getName().endsWith(".txt")) {
              FileUtils.copyFileToDirectory(add, cvAverageFolder);
            }
          }
        }

        return convertMRResultToJsonFile(inputFolder.getAbsolutePath() + File.separator + "part-r-00000");

      }
    } else {
      File v1 = new File(inputFolder.getAbsolutePath() + File.separator + CSV_FILE_NAME);
      int count = 0;
      while (!v1.exists() || count < 10) {
        Thread.sleep(100);
        count++;
      }
      String[] args = new String[]{"-op", "p", "-v1", inputFolder.getAbsolutePath() + File.separator + CSV_FILE_NAME,
        "-v2", inputFolder.getAbsolutePath() + File.separator + "list.csv", "-o", inputFolder.getAbsolutePath(), "-p", propertiesFile.getAbsolutePath()};
      BatchMain.main(args);
    }
    return null;
  }

  private File convertCSVJsonFile(String csvFile, String outputJsonFile) throws IOException {
    File f = new File(csvFile);
    if (f.exists()) {
      Map<String, Double> map = new HashMap<>();
      try (BufferedReader br = new BufferedReader(new FileReader(f))) {
        String line;
        while ((line = br.readLine()) != null) {
          String[] kv = line.split(",");
          map.put(kv[0], Double.valueOf(kv[1]));
        }
      }

      File jsonFile = new File(outputJsonFile);
      JSONObject jo = new JSONObject(map);
      try (PrintWriter out = new PrintWriter(jsonFile)) {
        out.print(jo.toJSONString());
      }
      return jsonFile;
    }
    return null;
  }

  private File convertMRResultToJsonFile(String mrPartPath) throws IOException {
    File parent = new File(mrPartPath).getParentFile();
    Map<String, Map<String, Double>> map = new HashMap<>();
    Map<String, Double> catSimMap;
    try (BufferedReader br = new BufferedReader(new FileReader(mrPartPath))) {
      String line;
      while ((line = br.readLine()) != null) {
        String[] kv = line.split("\t");
        String fileName = kv[0];
        String cat = kv[1];
        String sim = kv[2];
        catSimMap = map.get(fileName);
        if (catSimMap == null) {
          catSimMap = new HashMap<>();
        }
        catSimMap.put(cat, Double.valueOf(sim));
        map.put(fileName, catSimMap);
      }
    }

//    JSONArray ja = new JSONArray();
//    for (String fname : map.keySet()) {
//      Map<String, Double> catMap = map.get(fname);
//      JSONObject jo = new JSONObject(catMap);
//      ja.add(jo);
//    }
    File jsonFile = new File(parent.getAbsoluteFile() + File.separator + JSON_FILE_NAME);
    JSONObject jo = new JSONObject(map);
    try (PrintWriter out = new PrintWriter(jsonFile)) {
      out.print(jo.toJSONString());
    }
    return jsonFile;
  }

  private void convertMRResultToCSV(File mrPartPath, String outputPath, boolean calculateAvg) throws IOException {
    Map<String, Map<String, Double>> map = new HashMap<>();
    Map<String, Double> catSimMap;
    Map<String, List<Double>> avgMap = new HashMap<>();
    try (BufferedReader br = new BufferedReader(new FileReader(mrPartPath))) {
      String line;
      int count = 0;
      while ((line = br.readLine()) != null) {
        String[] kv = line.split("\t");
        String fileName = kv[0];
        String cat = kv[1];
        String sim = kv[2];
        catSimMap = map.get(fileName);
        if (catSimMap == null) {
          catSimMap = new HashMap<>();
        }
        catSimMap.put(cat, Double.valueOf(sim));
        map.put(fileName, catSimMap);
        List<Double> list = avgMap.get(cat);
        if (list == null) {
          list = new ArrayList<>();
        }
        list.add(Double.valueOf(sim));
        avgMap.put(cat, list);
      }
    }

    Set<String> fileNames = map.keySet();
    StringBuilder header = new StringBuilder();
    header.append("JobId").append(",");
    for (Map<String, Double> m : map.values()) {
      for (String c : m.keySet()) {
        header.append(c).append(",");
      }
      break;
    }
    header.deleteCharAt(header.length() - 1);
    header.setLength(header.length());

    File csvFile = new File(outputPath);
    try (PrintWriter out = new PrintWriter(csvFile)) {
      out.println(header);
      for (String fName : fileNames) {
        StringBuilder csvLine = new StringBuilder();

        csvLine.append(fName).append(",");
        catSimMap = map.get(fName);
        for (String cat : catSimMap.keySet()) {
          Double sim = catSimMap.get(cat);
          csvLine.append(sim).append(",");
        }
        csvLine.deleteCharAt(csvLine.length() - 1);
        csvLine.setLength(csvLine.length());
        out.println(csvLine.toString());
      }
    }
    if (calculateAvg) {
      csvFile = new File(mrPartPath.getParent() + File.separator + CSV_AVG_FILENAME);
      try (PrintWriter out = new PrintWriter(csvFile)) {
        Set<String> keys = avgMap.keySet();
        for (String k : keys) {
          List<Double> list = avgMap.get(k);
          Double sum = 0d;
          for (Double val : list) {
            if (!val.isNaN()) {
              sum += val;
            }
          }
          Double avg = sum / (list.size());
          out.println(k + "," + String.valueOf(avg));
        }
      }
    }

  }
}

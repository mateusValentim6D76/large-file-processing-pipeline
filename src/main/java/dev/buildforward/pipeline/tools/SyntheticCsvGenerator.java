package dev.buildforward.pipeline.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Random;


public final class SyntheticCsvGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SyntheticCsvGenerator.class);

    private static final Path OUTPUT = Path.of("data", "input.csv");
    private static final long TARGET_BYTES = 4608L * 1024 * 1024;
    private static final long SEED = 42L;
    private static final int BUFFER = 64 * 1024;
    private static final String[] DEPARTMENTS = {"Engineering", "Sales", "Marketing", "HR", "Finance", "Support",
    "Product", "Operations", "Legal", "IT"};

    public static void main(String[] args) throws IOException {
        try{
            generate();
        } catch (IOException e){
            logger.error("falha ao gerar o CSV em {} ", OUTPUT, e );
            System.exit(1);
        }

    }

    private static void generate() throws IOException {
        Files.createDirectories(OUTPUT.getParent());
        String[] dates = new String[730];
        LocalDate today = LocalDate.now();

        for (int i = 0; i < dates.length; i++){
            dates[i] = today.minusDays(i).toString(); // yyyy/MM/dd
        }

        StringBuilder row = new StringBuilder(64);
        long bytesWritten = 0;
        long rows = 0;
        long nextProgressAt = 100L * 1024 * 1024; //100mb
        long startNanos = System.nanoTime();
        Random random = new Random(SEED);

        try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(OUTPUT), StandardCharsets.UTF_8), BUFFER)) {
            String header = "employee_id,department,score,date\n";
            writer.write(header);
            bytesWritten += header.length();

            while (bytesWritten < TARGET_BYTES){
                int employeeId = random.nextInt(1_000_000) + 1; // 1...1.000.000
                String department = DEPARTMENTS[random.nextInt(DEPARTMENTS.length)];
                int score = random.nextInt(101); //0..100
                String date = dates[random.nextInt(dates.length)];

                row.setLength(0);
                row.append(employeeId).append(',')
                        .append(department).append(",")
                        .append(score).append(",")
                        .append(date).append("\n");

                writer.write(row.toString());
                bytesWritten += row.length();
                rows++;

                if (bytesWritten >= nextProgressAt){ //a cad 100mb
                    logger.info("Progresso: {} MB, {} linhas", bytesWritten / (1024 * 1024), rows);
                    nextProgressAt += 100L * 1024 * 1024;
                }
            }
        } //aqui o writer fecha flush do buffer restante + libera o arquivo

        long millis = (System.nanoTime() - startNanos) / 1_000_000;
        double mbPerSecond = (bytesWritten / (1024 * 1024)) / (millis / 1000.0);
        logger.info("Concluido: {} linhas, {} MB, {} ms, {} MB/s", rows, bytesWritten / (1024.0 * 1024.0), millis, mbPerSecond);
    }
}

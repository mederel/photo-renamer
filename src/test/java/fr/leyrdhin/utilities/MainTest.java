package fr.leyrdhin.utilities;


import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.matchesRegex;

class MainTest {

    @TempDir
    Path temporaryFolder;

    Path imagesDir;

    private void setUp(String sourceSubDir) throws Exception {
        Path srcDir = Path.of("src", "test", "resources", sourceSubDir);
        imagesDir = temporaryFolder.resolve(sourceSubDir);
        FileUtils.copyDirectory(srcDir.toFile(), imagesDir.toFile());
    }

    @Test
    void testMain() throws Exception {
        setUp("images");

        try (Stream<Path> files = Files.list(imagesDir)) {
            System.out.println(files.toList());
        }

        Main.main(new String[]{imagesDir.toString()});

        try (Stream<Path> files = Files.list(imagesDir)) {
            List<String> fileNames = files.map(Path::getFileName).map(Path::toString).toList();
            System.out.println(fileNames);
            assertThat(fileNames, everyItem(matchesRegex("\\d{8}_\\d{6}\\.(jpg|mp4|arw)")));
        }
    }


    @Test
    void similarDates() throws Exception {
        setUp("images_similar_dates");

        try (Stream<Path> files = Files.list(imagesDir)) {
            System.out.println(files.toList());
        }

        Main.main(new String[]{imagesDir.toString()});

        try (Stream<Path> files = Files.list(imagesDir)) {
            List<String> fileNames = files.map(Path::getFileName).map(Path::toString).toList();
            System.out.println(fileNames);
            assertThat(fileNames, everyItem(matchesRegex("20250106_232[678]\\d{2}\\.(jpg|mp4|arw)")));
        }
    }
}

package fr.leyrdhin.utilities;


import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    @BeforeEach
    void setUp() throws Exception {
        Path srcDir = Path.of("src", "test", "resources", "images");
        imagesDir = temporaryFolder.resolve("images");
        FileUtils.copyDirectory(srcDir.toFile(), imagesDir.toFile());
    }

    @Test
    void testMain() throws Exception {
        try (Stream<Path> files = Files.list(imagesDir)) {
            System.out.println(files.toList());
        }

        Main.main(new String[]{imagesDir.toString()});

        try (Stream<Path> files = Files.list(imagesDir)) {
            List<String> fileNames = files.map(Path::getFileName).map(Path::toString).toList();
            System.out.println(fileNames);
            assertThat(fileNames, everyItem(matchesRegex("\\d{8}_\\d{6}\\.(jpg|mp4)")));
        }
    }
}

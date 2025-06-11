package fr.leyrdhin.utilities;


import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

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


        Path renamedVid = imagesDir.resolve("20241214_112728.mp4");
        FFprobe ffprobe = new FFprobe("/usr/bin/ffprobe");
        FFmpegProbeResult probeResult = ffprobe.probe(renamedVid.toAbsolutePath().toString());
        assertThat(probeResult.getStreams().get(0).width, equalTo(1280));
        assertThat(probeResult.getStreams().get(0).height, equalTo(720));
    }


    @Test
    void similarDatesWinter() throws Exception {
        setUp("images_similar_dates_winter");

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


    @Test
    void similarDatesSummer() throws Exception {
        setUp("images_similar_dates_summer");

        try (Stream<Path> files = Files.list(imagesDir)) {
            System.out.println(files.toList());
        }

        Main.main(new String[]{imagesDir.toString()});

        try (Stream<Path> files = Files.list(imagesDir)) {
            List<String> fileNames = files.map(Path::getFileName).map(Path::toString).toList();
            System.out.println(fileNames);
            assertThat(fileNames, everyItem(matchesRegex("20250509_15\\d{4}\\.(jpg|mp4|arw)")));
        }
    }

    @Test
    void verticalVid() throws Exception {
        setUp("vertical_vid");

        try (Stream<Path> files = Files.list(imagesDir)) {
            System.out.println(files.toList());
        }

        Main.main(new String[]{imagesDir.toString()});

        Path renamedVid = imagesDir.resolve("20250611_223358.mp4");
        assertTrue(Files.exists(renamedVid));

        FFprobe ffprobe = new FFprobe("/usr/bin/ffprobe");
        FFmpegProbeResult probeResult = ffprobe.probe(renamedVid.toAbsolutePath().toString());
        assertThat(probeResult.getStreams().get(0).width, equalTo(720));
        assertThat(probeResult.getStreams().get(0).height, equalTo(1280));
    }
}

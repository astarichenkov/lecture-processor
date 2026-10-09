package com.lectureprocessor.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import com.lectureprocessor.config.AppProperties;
import com.lectureprocessor.support.TestProperties;
import com.lectureprocessor.video.FfmpegService;

class FfmpegServiceTest {

    private final ProcessExecutor executor = mock(ProcessExecutor.class);
    private final AppProperties properties = TestProperties.defaults("./target/test-data");
    private final FfmpegService service = new FfmpegService(executor, properties);

    @Test
    void extractAudioBuildsSafeArgumentList(@TempDir Path dir) throws IOException {
        Path video = Files.createFile(dir.resolve("lecture.mp4"));
        Path wav = dir.resolve("audio.wav");
        Files.write(wav, new byte[] { 1, 2, 3 });

        when(executor.execute(any(), any(), any())).thenReturn(
                new ProcessResult(0, "", "", Duration.ofMillis(5), false));

        service.extractAudio(video, wav);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> commandCaptor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(executor).execute(commandCaptor.capture(), any(), any());
        assertThat(commandCaptor.getValue()).contains("ffmpeg").contains("-vn").contains("16000");
    }

    @Test
    void probeDurationParsesSeconds(@TempDir Path dir) throws IOException {
        Path video = Files.createFile(dir.resolve("lecture.mp4"));
        when(executor.execute(any(), any(), any())).thenReturn(
                new ProcessResult(0, "123.5\n", "", Duration.ofMillis(5), false));

        assertThat(service.probeDurationMs(video)).isEqualTo(123500);
    }

    @Test
    void probeDurationReturnsZeroOnFailure(@TempDir Path dir) throws IOException {
        Path video = Files.createFile(dir.resolve("lecture.mp4"));
        when(executor.execute(any(), any(), any())).thenReturn(
                new ProcessResult(1, "", "error", Duration.ofMillis(5), false));
        assertThat(service.probeDurationMs(video)).isZero();
    }

    @Test
    void extractAudioFailsWhenNoOutput(@TempDir Path dir) throws IOException {
        Path video = Files.createFile(dir.resolve("lecture.mp4"));
        Path wav = dir.resolve("audio.wav");
        when(executor.execute(any(), any(), any())).thenReturn(
                new ProcessResult(0, "", "", Duration.ofMillis(5), false));

        assertThatThrownBy(() -> service.extractAudio(video, wav))
                .isInstanceOf(ProcessExecutionException.class);
    }
}

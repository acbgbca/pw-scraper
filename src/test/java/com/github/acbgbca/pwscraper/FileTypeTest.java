package com.github.acbgbca.pwscraper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class FileTypeTest {

  @ParameterizedTest
  @ValueSource(strings = {"image.png", "image.PNG", "img.pdf.png"})
  void testGetFileTypePng(String value) {
    assertEquals(FileType.PNG, FileType.getFileType(value));
  }

  @ParameterizedTest
  @ValueSource(strings = {"image.jpg", "image.JPG", "img.pdf.jpg", "img.jpeg"})
  void testGetFileTypeJpeg(String value) {
    assertEquals(FileType.JPEG, FileType.getFileType(value));
  }

  @ParameterizedTest
  @ValueSource(strings = {"image.pdf", "image.PDF", "img.png.pdf"})
  void testGetFileTypePdf(String value) {
    assertEquals(FileType.PDF, FileType.getFileType(value));
  }

  @ParameterizedTest
  @ValueSource(strings = {"content", "content.pngd", "", "content.html"})
  void testGetFileTypeText(String value) {
    assertEquals(FileType.TEXT, FileType.getFileType(value));
  }
}

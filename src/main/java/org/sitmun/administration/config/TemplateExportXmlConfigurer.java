package org.sitmun.administration.config;

import java.io.IOException;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.sitmun.administration.controller.dto.TemplateExportRequestDto;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TemplateExportXmlConfigurer implements WebMvcConfigurer {

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.add(0, new TemplateExportXmlHttpMessageConverter());
  }

  private static final class TemplateExportXmlHttpMessageConverter
      extends AbstractHttpMessageConverter<TemplateExportRequestDto> {

    private TemplateExportXmlHttpMessageConverter() {
      super(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
      return TemplateExportRequestDto.class == clazz;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
      return false;
    }

    @Override
    protected TemplateExportRequestDto readInternal(
        Class<? extends TemplateExportRequestDto> clazz, HttpInputMessage inputMessage)
        throws IOException, HttpMessageNotReadableException {
      XMLInputFactory factory = XMLInputFactory.newFactory();
      factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
      factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

      try {
        XMLStreamReader reader = factory.createXMLStreamReader(inputMessage.getBody());
        try {
          return readRequest(reader);
        } finally {
          reader.close();
        }
      } catch (XMLStreamException | NumberFormatException exception) {
        throw new HttpMessageNotReadableException(
            "Invalid template export XML", exception, inputMessage);
      }
    }

    private TemplateExportRequestDto readRequest(XMLStreamReader reader) throws XMLStreamException {
      String output = null;
      String template = null;
      Integer taskId = null;
      Integer templateTaskId = null;
      Integer applicationId = null;
      Integer territoryId = null;

      while (reader.hasNext()) {
        if (reader.next() != XMLStreamConstants.START_ELEMENT) {
          continue;
        }

        switch (reader.getLocalName()) {
          case "templateExportRequest" -> {}
          case "output" -> output = reader.getElementText();
          case "template" -> template = reader.getElementText();
          case "taskId" -> taskId = parseInteger(reader.getElementText());
          case "templateTaskId" -> templateTaskId = parseInteger(reader.getElementText());
          case "applicationId" -> applicationId = parseInteger(reader.getElementText());
          case "territoryId" -> territoryId = parseInteger(reader.getElementText());
          default -> throw new XMLStreamException("Unknown element: " + reader.getLocalName());
        }
      }

      return new TemplateExportRequestDto(
          output, template, taskId, templateTaskId, applicationId, territoryId);
    }

    private Integer parseInteger(String value) {
      return value == null || value.isBlank() ? null : Integer.valueOf(value.trim());
    }

    @Override
    protected void writeInternal(
        TemplateExportRequestDto request, HttpOutputMessage outputMessage)
        throws HttpMessageNotWritableException {
      throw new HttpMessageNotWritableException("Template export XML responses are not supported");
    }
  }
}

package gov.cms.mat.cql_elm_translation.controllers;


import gov.cms.mat.cql_elm_translation.service.MatXmlConversionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/cql/marshaller")
@Tag(name = "XmlMarshal-Controller", description = "API for converting stuff - TODO.")
@Slf4j
public class CqlXmlMarshalController {

    // Todo This service is Not necessary, as it only takes MAT's Measure Xml and converts it into CqlModel suitable for MAT.
    private final MatXmlConversionService matXmlConversionService;

    public CqlXmlMarshalController(MatXmlConversionService matXmlConversionService) {
        this.matXmlConversionService = matXmlConversionService;
    }

    @PutMapping(consumes = "text/plain", produces = "text/plain")
    public String convertXmlToCql(@RequestBody String xml) {

        return matXmlConversionService.processCqlXml(xml);
    }
}

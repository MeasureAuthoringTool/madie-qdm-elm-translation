<!DOCTYPE>
<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
        <title>${model.measureInformation.ecqmTitle} ${model.measureInformation.ecqmVersionNumber}</title>
        <#include "human_readable_css.ftl" />
    </head>
    <body>
        <#include "measure_information_table.ftl"/>
        <#include "table_of_contents.ftl"/>
        <#include "divider.ftl" />
        <#include "population_criteria_section.ftl" />
<#--TODO        <#include "definition_section.ftl" />-->
<#--TODO        <#include "function_section.ftl" />-->
<#--TODO        <#include "terminology_section.ftl" />-->
<#--TODO        <#include "data_criteria_section.ftl" />-->
<#--TODO        <#include "supplemental_data_elements_section.ftl" />-->
<#--TODO        <#include "risk_adjustment_variables_section.ftl" />-->
        <#include "measure_set_table.ftl" />
    </body>
</html>

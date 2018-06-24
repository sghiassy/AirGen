package io.swagger.codegen.swift4;

import io.swagger.codegen.AbstractOptionsTest;
import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.languages.AirGenCodegen;
import io.swagger.codegen.options.AirGenOptionsProvider;
import mockit.Expectations;
import mockit.Tested;

public class AirGenOptionsTest extends AbstractOptionsTest {

    @Tested
    private AirGenCodegen clientCodegen;

    public AirGenOptionsTest() {
        super(new AirGenOptionsProvider());
    }

    @Override
    protected CodegenConfig getCodegenConfig() {
        return clientCodegen;
    }

    @SuppressWarnings("unused")
    @Override
    protected void setExpectations() {
        new Expectations(clientCodegen) {
            {
                clientCodegen.setSortParamsByRequiredFlag(Boolean.valueOf(AirGenOptionsProvider.SORT_PARAMS_VALUE));
                times = 1;
                clientCodegen.setProjectName(AirGenOptionsProvider.PROJECT_NAME_VALUE);
                times = 1;
                clientCodegen.setResponseAs(AirGenOptionsProvider.RESPONSE_AS_VALUE.split(","));
                times = 1;
                clientCodegen.setUnwrapRequired(Boolean.valueOf(AirGenOptionsProvider.UNWRAP_REQUIRED_VALUE));
                times = 1;
                clientCodegen.setObjcCompatible(Boolean.valueOf(AirGenOptionsProvider.OBJC_COMPATIBLE_VALUE));
                times = 1;
                clientCodegen.setLenientTypeCast(Boolean.valueOf(AirGenOptionsProvider.LENIENT_TYPE_CAST_VALUE));
                times = 1;
            }
        };
    }
}

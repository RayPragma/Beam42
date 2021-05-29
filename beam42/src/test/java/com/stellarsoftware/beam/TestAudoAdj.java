package com.stellarsoftware.beam;

import com.stellarsoftware.beam.core.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class TestAudoAdj {

    @Test
    public void testAuto1() {
        Globals.reg = new Registry(null);  // create and load registry
        OPTDataModel optDataModel = new OPTDataModel();
        Assert.assertTrue(optDataModel.bLoadFile(new File("../Examples/AUTO1.OPT")));
        RAYDataModel rayDataModel = new RAYDataModel();
        Assert.assertTrue(rayDataModel.bLoadFile(new File("../Examples/AUTO1.RAY")));
        B4DataParser b4DataParser = new B4DataParser(optDataModel, rayDataModel, null);
        b4DataParser.parse(true);
//        new InOut(optDataModel, rayDataModel);
        AutoAdjuster autoAdjuster = new AutoAdjuster(optDataModel, rayDataModel, null, null, Globals.RT13);
        autoAdjuster.run();
        System.out.println(optDataModel.getTableString());
        Assert.assertEquals("3.12613", optDataModel.getFieldTrim(optDataModel.oI2F(OPTDataModel.getOptFieldAttrib("Z")), 5));
        System.out.println(rayDataModel.getTableString());
    }

}

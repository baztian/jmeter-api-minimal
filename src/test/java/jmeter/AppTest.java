
package jmeter;

/*
 * #%L
 * jmeter-api-minimal
 * %%
 * Copyright (C) 2015 Bastian Bowe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.gui.TestPlanGui;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.threads.gui.ThreadGroupGui;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AppTest {

    private StandardJMeterEngine jmeter;

    private HashTree jmeterTree;


    @Before
    public void setUp() {
        jmeter = new StandardJMeterEngine();
        File jmeterHome = new File(System.getProperty("jmeterHome", "target" + File.separator + "apache-jmeter"));
        if (!jmeterHome.exists()) {
            throw new IllegalStateException("JMeter home folder not found: " + jmeterHome.getPath());
        }
        JMeterUtils.setJMeterHome(jmeterHome.getPath());
        String jmeterProperties =
            new File(jmeterHome.getPath() + File.separator + "bin" + File.separator + "jmeter.properties").getPath();
        JMeterUtils.loadJMeterProperties(jmeterProperties);
        // Dirty hack to make sure JMeter ClassFinder does find the function classes. Otherwise functions in .jmx files
        // wont' work. See https://bz.apache.org/bugzilla/show_bug.cgi?id=50585
        JMeterUtils.setProperty("search_paths", System.getProperty("java.class.path"));
        JMeterUtils.initLocale();
        jmeterTree = new HashTree();
    }


    @After
    public void tearDown() {
        String jmxFileName = "target" + File.separator + "run.jmx";
        try (OutputStream outputStream = new FileOutputStream(jmxFileName)) {
            SaveService.saveTree(jmeterTree, outputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        jmeter.configure(jmeterTree);
        jmeter.run();
    }


    @Test
    public void testApp() {
        TestPlan testPlan = new TestPlan();
        testPlan.setName("Test Plan");
        testPlan.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());
        HashTree testPlanTree = jmeterTree.add(testPlan);

        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName("Thread Group");
        threadGroup.setNumThreads(1);
        threadGroup.setProperty(TestElement.GUI_CLASS, ThreadGroupGui.class.getName());
        LoopController loopCtrl = new LoopController();
        loopCtrl.setLoops(1);
        threadGroup.setSamplerController(loopCtrl);
        HashTree threadGroupTree = testPlanTree.add(threadGroup);

        HTTPSampler httpSampler = new HTTPSampler();
        httpSampler.setName("HTTP Request");
        httpSampler.setDomain("www.google.com");
        httpSampler.setPort(80);
        httpSampler.setPath("/q=${__log(example)}");
        httpSampler.setMethod("GET");
        httpSampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());
        threadGroupTree.add(httpSampler);
    }
}

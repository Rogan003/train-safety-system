package com.ftn.sbnz.train.service.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;

import com.ftn.sbnz.train.model.facts.GradientSegment;
import com.ftn.sbnz.train.model.facts.Infrastructure;
import com.ftn.sbnz.train.model.facts.TrainStatus;

import static org.junit.Assert.assertEquals;


public class AccumulateRulesTest {

    private KieSession kSession;

    @Before
    public void setUp() {
        KieHelper helper = new KieHelper();
        helper.addResource(
                ResourceFactory.newClassPathResource("rules/accumulate/accumulate-rules.drl",
                        getClass().getClassLoader()),
                ResourceType.DRL);
        kSession = helper.build().newKieSession();
        KieServices.Factory.get();
    }

    @After
    public void tearDown() {
        if (kSession != null) {
            kSession.dispose();
        }
    }

    /**
     * Three segments fully fall in the 1000m window ahead of the train, with
     * gradients 2, 4 and 6 -> average must be 4.0.
     */
    @Test
    public void testRule10_AverageGradientOfNext1000m() {
        TrainStatus train = new TrainStatus();
        train.setPosition(0.0);

        Infrastructure infra = new Infrastructure();
        infra.setGradient(0.0); // different from the to-be-computed average

        // Three 300m segments inside [0, 1000)
        GradientSegment g1 = new GradientSegment(100.0, 400.0, 2.0);
        GradientSegment g2 = new GradientSegment(400.0, 700.0, 4.0);
        GradientSegment g3 = new GradientSegment(700.0, 900.0, 6.0);

        kSession.insert(train);
        kSession.insert(infra);
        kSession.insert(g1);
        kSession.insert(g2);
        kSession.insert(g3);

        int fired = kSession.fireAllRules();

        assertEquals("Rule 10 should fire exactly once", 1, fired);
        assertEquals("Infrastructure gradient should be average of segments (2+4+6)/3 = 4.0",
                4.0, infra.getGradient(), 0.0001);
    }

    /**
     * Segments outside the [pos, pos+1000] window must be ignored.
     */
    @Test
    public void testRule10_IgnoresSegmentsOutsideWindow() {
        TrainStatus train = new TrainStatus();
        train.setPosition(0.0);

        Infrastructure infra = new Infrastructure();
        infra.setGradient(0.0);

        // Inside window
        GradientSegment inside = new GradientSegment(200.0, 500.0, 10.0);
        // Behind train (endPosition <= position) -> excluded
        GradientSegment behind = new GradientSegment(-500.0, -100.0, 99.0);
        // Far ahead, starting after pos+1000 -> excluded
        GradientSegment farAhead = new GradientSegment(1500.0, 1800.0, 50.0);

        kSession.insert(train);
        kSession.insert(infra);
        kSession.insert(inside);
        kSession.insert(behind);
        kSession.insert(farAhead);

        kSession.fireAllRules();

        assertEquals("Only the in-window segment should contribute to the average",
                10.0, infra.getGradient(), 0.0001);
    }

    /**
     * Rule 10 has {@code no-loop true} and an explicit guard
     * {@code doubleValue != $currentGrad} - it must not fire again once the
     * infrastructure already reflects the computed average.
     */
    @Test
    public void testRule10_NoLoopWhenAverageAlreadySet() {
        TrainStatus train = new TrainStatus();
        train.setPosition(0.0);

        Infrastructure infra = new Infrastructure();
        // Pre-set the gradient to the value the rule would compute
        infra.setGradient(5.0);

        GradientSegment g1 = new GradientSegment(100.0, 400.0, 5.0);
        GradientSegment g2 = new GradientSegment(400.0, 700.0, 5.0);

        kSession.insert(train);
        kSession.insert(infra);
        kSession.insert(g1);
        kSession.insert(g2);

        int fired = kSession.fireAllRules();

        assertEquals("Rule 10 must not fire when current gradient already equals the average",
                0, fired);
        assertEquals(5.0, infra.getGradient(), 0.0001);
    }
}

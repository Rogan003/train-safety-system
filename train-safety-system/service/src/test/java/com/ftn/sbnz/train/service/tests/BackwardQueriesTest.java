package com.ftn.sbnz.train.service.tests;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;

import com.ftn.sbnz.train.model.enums.BrakeTestStatus;
import com.ftn.sbnz.train.model.enums.DoorStatus;
import com.ftn.sbnz.train.model.enums.InteractionType;
import com.ftn.sbnz.train.model.enums.SifaStatus;
import com.ftn.sbnz.train.model.events.DriverActivity;
import com.ftn.sbnz.train.model.facts.Car;
import com.ftn.sbnz.train.model.facts.MovementAuthority;
import com.ftn.sbnz.train.model.facts.RouteNode;
import com.ftn.sbnz.train.model.facts.TrainStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class BackwardQueriesTest {

    private KieSession kSession;

    @Before
    public void setUp() {
        KieHelper helper = new KieHelper();
        helper.addResource(
                ResourceFactory.newClassPathResource("rules/backward/backward-queries.drl",
                        getClass().getClassLoader()),
                ResourceType.DRL);

        KieBaseConfiguration kbConf =
                KieServices.Factory.get().newKieBaseConfiguration();
        kbConf.setOption(EventProcessingOption.STREAM);
        kSession = helper.build(kbConf).newKieSession();
    }

    @After
    public void tearDown() {
        if (kSession != null) {
            kSession.dispose();
        }
    }

    @Test
    public void testSafeRouteTree() {
        RouteNode leaf1 = new RouteNode("L1", true);
        RouteNode leaf2 = new RouteNode("L2", true);
        
        RouteNode switch1 = new RouteNode("S1", true);
        switch1.setLeft(leaf1);
        switch1.setRight(leaf2);
        
        kSession.insert(leaf1);
        kSession.insert(leaf2);
        kSession.insert(switch1);
        
        QueryResults res = kSession.getQueryResults("isSafeRouteTree", switch1);
        assertTrue("Should find a safe route", res.size() > 0);
        
        // now make leaves not clear
        leaf1.setClear(false);
        leaf2.setClear(false);
        kSession.update(kSession.getFactHandle(leaf1), leaf1);
        kSession.update(kSession.getFactHandle(leaf2), leaf2);
        
        QueryResults res2 = kSession.getQueryResults("isSafeRouteTree", switch1);
        assertTrue("Should not find a safe route", res2.size() == 0);
    }
    
    @Test
    public void testSafeToDepart() {
        TrainStatus ts = new TrainStatus();
        ts.setPosition(100.0);
        ts.setBrakePressure(5.0);
        ts.setCompressorOperational(true);
        ts.setSifaStatus(SifaStatus.OPERATIONAL);
        
        MovementAuthority ma = new MovementAuthority();
        ma.setEoa(500.0);
        
        Car loco = new Car("L1", true, 90000, 100);
        loco.setDoorStatus(DoorStatus.LOCKED);
        loco.setBrakeTest(BrakeTestStatus.PASSED);
        
        Car c1 = new Car("C1", false, 40000, 80);
        c1.setDoorStatus(DoorStatus.LOCKED);
        c1.setBrakeTest(BrakeTestStatus.PASSED);
        loco.setNextCar(c1);
        
        DriverActivity da = new DriverActivity(InteractionType.SIFA_RESET);
        
        kSession.insert(ts);
        kSession.insert(ma);
        kSession.insert(loco);
        kSession.insert(c1);
        kSession.insert(da);
        
        QueryResults res = kSession.getQueryResults("safeToDepart");
        assertTrue("Train should be safe to depart", res.size() > 0);
        
        // Break a condition
        c1.setDoorStatus(DoorStatus.OPEN);
        kSession.update(kSession.getFactHandle(c1), c1);
        QueryResults res2 = kSession.getQueryResults("safeToDepart");
        assertTrue("Train should NOT be safe to depart", res2.size() == 0);
    }
}

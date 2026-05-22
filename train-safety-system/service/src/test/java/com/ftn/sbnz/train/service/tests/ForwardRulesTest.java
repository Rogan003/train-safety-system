package com.ftn.sbnz.train.service.tests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;

import com.ftn.sbnz.train.model.enums.DoorStatus;
import com.ftn.sbnz.train.model.enums.WarningType;
import com.ftn.sbnz.train.model.events.EmergencyBrakeCommand;
import com.ftn.sbnz.train.model.events.ServiceBrakeCommand;
import com.ftn.sbnz.train.model.events.Warning;
import com.ftn.sbnz.train.model.facts.Balise;
import com.ftn.sbnz.train.model.facts.BrakingCurve;
import com.ftn.sbnz.train.model.facts.Car;
import com.ftn.sbnz.train.model.facts.CriticalSafetyBreach;
import com.ftn.sbnz.train.model.facts.Infrastructure;
import com.ftn.sbnz.train.model.facts.MovementAuthority;
import com.ftn.sbnz.train.model.facts.TrainStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class ForwardRulesTest {

    private KieSession kSession;

    @Before
    public void setUp() {
        KieHelper helper = new KieHelper();
        helper.addResource(
                ResourceFactory.newClassPathResource("rules/forward/forward-rules.drl",
                        getClass().getClassLoader()),
                ResourceType.DRL);

        KieBaseConfiguration kbConf =
                KieServices.Factory.get().newKieBaseConfiguration();
        kbConf.setOption(EventProcessingOption.STREAM);
        kSession = helper.build(kbConf).newKieSession();

        KieServices.Factory.get();
    }

    @After
    public void tearDown() {
        if (kSession != null) {
            kSession.dispose();
        }
    }

    private BrakingCurve defaultBrakingCurve() {
        BrakingCurve bc = new BrakingCurve();
        bc.setvWarning(100.0);
        bc.setvServiceBrake(110.0);
        bc.setvEmergencyBrake(120.0);
        return bc;
    }

    private <T> T findFact(Class<T> clazz) {
        Collection<FactHandle> handles = kSession.getFactHandles(o -> clazz.isInstance(o));
        if (handles.isEmpty()) {
            return null;
        }
        return clazz.cast(kSession.getObject(handles.iterator().next()));
    }

    private <T> List<T> findFacts(Class<T> clazz) {
        List<T> result = new ArrayList<>();
        for (FactHandle h : kSession.getFactHandles(o -> clazz.isInstance(o))) {
            result.add(clazz.cast(kSession.getObject(h)));
        }
        return result;
    }

    /** Rule 1: balise at/before train position is consumed and MA gets updated. */
    @Test
    public void testRule1_BaliseUpdatesMovementAuthority() {
        TrainStatus train = new TrainStatus();
        train.setPosition(500.0);
        train.setSpeed(50.0);

        Balise balise = new Balise("B1", 400.0, 2000.0, 80.0);
        Infrastructure infra = new Infrastructure();
        infra.getBalises().add(balise);

        MovementAuthority ma = new MovementAuthority(1000.0, 60.0);

        kSession.insert(train);
        kSession.insert(infra);
        kSession.insert(ma);

        int fired = kSession.fireAllRules();

        assertTrue("Rule 1 should have fired", fired >= 1);
        assertTrue("Balise should be marked as consumed", balise.isConsumed());
        assertEquals("MA EOA should be updated from balise", 2000.0, ma.getEoa(), 0.0001);
        assertEquals("MA target speed should be updated from balise", 80.0, ma.getTargetSpeed(), 0.0001);
        assertEquals("Current balise id should be set on infrastructure", "B1", infra.getCurrentBaliseId());
    }

    /** Rule 1 should NOT fire when balise position is ahead of the train. */
    @Test
    public void testRule1_BaliseAheadOfTrain_NotConsumed() {
        TrainStatus train = new TrainStatus();
        train.setPosition(100.0);
        train.setSpeed(50.0);

        Balise balise = new Balise("B-future", 5000.0, 9000.0, 90.0);
        Infrastructure infra = new Infrastructure();
        infra.getBalises().add(balise);

        MovementAuthority ma = new MovementAuthority(1000.0, 60.0);

        kSession.insert(train);
        kSession.insert(infra);
        kSession.insert(ma);

        kSession.fireAllRules();

        assertFalse("Balise ahead of train must not be consumed", balise.isConsumed());
        assertEquals("MA should remain unchanged", 1000.0, ma.getEoa(), 0.0001);
    }

    /** Rule 3: train speed above the warning threshold inserts an OVERSPEED warning. */
    @Test
    public void testRule3_OverspeedWarningInserted() {
        BrakingCurve bc = defaultBrakingCurve();
        TrainStatus train = new TrainStatus();
        train.setSpeed(105.0); // above vWarning=100

        kSession.insert(bc);
        kSession.insert(train);

        kSession.fireAllRules();

        Warning w = findFact(Warning.class);
        assertNotNull("Overspeed warning should be inserted", w);
        assertEquals(WarningType.OVERSPEED, w.getType());
    }

    /** Rule 3b: once speed drops below 0.9 * vWarning, the overspeed warning is cleared. */
    @Test
    public void testRule3b_OverspeedWarningCleared() {
        BrakingCurve bc = defaultBrakingCurve();
        TrainStatus train = new TrainStatus();
        train.setSpeed(105.0);

        FactHandle bcH = kSession.insert(bc);
        FactHandle trainH = kSession.insert(train);

        kSession.fireAllRules();
        assertNotNull(findFact(Warning.class));

        // slow down well below threshold (90% of 100 = 90)
        train.setSpeed(85.0);
        kSession.update(trainH, train);
        kSession.fireAllRules();

        assertEquals("Overspeed warning should be cleared",
                0, findFacts(Warning.class).size());
        // silence "unused" warnings
        assertNotNull(bcH);
    }

    /** Rule 4: overspeed above SBD plus no driver braking inserts ServiceBrakeCommand. */
    @Test
    public void testRule4_ServiceBrakeCommandIssued() {
        BrakingCurve bc = defaultBrakingCurve();
        TrainStatus train = new TrainStatus();
        train.setSpeed(115.0);  // > vServiceBrake=110
        train.setBrake(0.0);    // driver not braking

        kSession.insert(bc);
        kSession.insert(train);

        kSession.fireAllRules();

        ServiceBrakeCommand cmd = findFact(ServiceBrakeCommand.class);
        assertNotNull("ServiceBrakeCommand should be issued", cmd);
        assertTrue("Reason should reference Rule 4", cmd.getReason().startsWith("Rule 4"));
    }

    /** Rule 5: speed above EBD triggers an EmergencyBrakeCommand. */
    @Test
    public void testRule5_EmergencyBrake() {
        BrakingCurve bc = defaultBrakingCurve();
        TrainStatus train = new TrainStatus();
        train.setSpeed(125.0); // > vEmergencyBrake=120

        kSession.insert(bc);
        kSession.insert(train);

        kSession.fireAllRules();

        EmergencyBrakeCommand cmd = findFact(EmergencyBrakeCommand.class);
        assertNotNull("EmergencyBrakeCommand should be issued", cmd);
        assertTrue(cmd.getReason().startsWith("Rule 5"));
    }

    /** Rule 6 + Rule 7: doors open while moving creates a breach and cuts traction. */
    @Test
    public void testRule6and7_DoorsOpenCreatesBreachAndCutsTraction() {
        TrainStatus train = new TrainStatus();
        train.setSpeed(10.0);
        train.setTractionActive(true);
        train.setThrottle(0.5);

        Car car = new Car("C1", false, 40000.0, 0.0);
        car.setDoorStatus(DoorStatus.OPEN);

        kSession.insert(train);
        kSession.insert(car);

        kSession.fireAllRules();

        CriticalSafetyBreach breach = findFact(CriticalSafetyBreach.class);
        assertNotNull("CriticalSafetyBreach should be inserted (Rule 6)", breach);
        assertTrue(breach.getReason().startsWith("Doors open"));

        // Rule 7 reacts to any breach: traction should be cut.
        assertFalse("Traction should be cut (Rule 7)", train.isTractionActive());
        assertEquals("Throttle should be zero", 0.0, train.getThrottle(), 0.0001);
    }

    /** Rule 6b: once all doors are locked the doors-breach is removed. */
    @Test
    public void testRule6b_BreachClearedWhenDoorsLocked() {
        TrainStatus train = new TrainStatus();
        train.setSpeed(10.0);

        Car car = new Car("C1", false, 40000.0, 0.0);
        car.setDoorStatus(DoorStatus.OPEN);

        kSession.insert(train);
        FactHandle carH = kSession.insert(car);

        kSession.fireAllRules();
        assertNotNull(findFact(CriticalSafetyBreach.class));

        car.setDoorStatus(DoorStatus.LOCKED);
        kSession.update(carH, car);
        kSession.fireAllRules();

        assertEquals("Doors breach should be cleared once all doors are LOCKED",
                0, findFacts(CriticalSafetyBreach.class).size());
    }
}

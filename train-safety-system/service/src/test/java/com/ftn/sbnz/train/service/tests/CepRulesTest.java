package com.ftn.sbnz.train.service.tests;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.drools.core.time.SessionPseudoClock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;

import com.ftn.sbnz.train.model.enums.InteractionType;
import com.ftn.sbnz.train.model.enums.SifaStatus;
import com.ftn.sbnz.train.model.enums.WarningType;
import com.ftn.sbnz.train.model.events.DriverActivity;
import com.ftn.sbnz.train.model.events.EmergencyBrakeCommand;
import com.ftn.sbnz.train.model.events.ServiceBrakeCommand;
import com.ftn.sbnz.train.model.events.Warning;
import com.ftn.sbnz.train.model.events.WheelSlipEvent;
import com.ftn.sbnz.train.model.facts.TrainStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CepRulesTest {

    private KieSession kSession;
    private SessionPseudoClock clock;

    @Before
    public void setUp() {
        KieHelper helper = new KieHelper();
        helper.addResource(
                ResourceFactory.newClassPathResource("rules/cep/cep-rules.drl",
                        getClass().getClassLoader()),
                ResourceType.DRL);

        KieBaseConfiguration kbConf =
                KieServices.Factory.get().newKieBaseConfiguration();
        kbConf.setOption(EventProcessingOption.STREAM);
        
        KieSessionConfiguration ksConf = KieServices.Factory.get().newKieSessionConfiguration();
        ksConf.setOption(ClockTypeOption.get("pseudo"));
        
        kSession = helper.build(kbConf).newKieSession(ksConf, null);
        clock = kSession.getSessionClock();
    }

    @After
    public void tearDown() {
        if (kSession != null) {
            kSession.dispose();
        }
    }

    private <T> T findFact(Class<T> clazz) {
        Collection<FactHandle> handles = kSession.getFactHandles(o -> clazz.isInstance(o));
        if (handles.isEmpty()) {
            return null;
        }
        return clazz.cast(kSession.getObject(handles.iterator().next()));
    }

    @Test
    public void testRule11_SIFA_Vigilance() {
        DriverActivity da = new DriverActivity(InteractionType.SIFA_RESET);
        da.setEventTime(new Date(clock.getCurrentTime()));
        kSession.insert(da);
        clock.advanceTime(40, TimeUnit.SECONDS);

        TrainStatus ts = new TrainStatus();
        ts.setSifaStatus(SifaStatus.OPERATIONAL);
        ts.setSpeed(50);
        kSession.insert(ts);

        kSession.fireAllRules();
        
        Warning warning = findFact(Warning.class);
        assertNotNull("Warning should be generated if no driver activity is present in window and train is moving", warning);
        assertEquals(WarningType.SIFA, warning.getType());
    }

    @Test
    public void testRule11b_ClearSifaWarning() {
        Warning w = new Warning(WarningType.SIFA, "Test");
        w.setEventTime(new Date(clock.getCurrentTime()));
        kSession.insert(w);
        kSession.fireAllRules();
        
        assertNotNull(findFact(Warning.class));
        
        clock.advanceTime(1, TimeUnit.SECONDS);
        
        DriverActivity da = new DriverActivity(InteractionType.SIFA_RESET);
        da.setEventTime(new Date(clock.getCurrentTime()));
        kSession.insert(da);
        kSession.fireAllRules();
        
        assertNull("Warning should be cleared by driver activity", findFact(Warning.class));
    }

    @Test
    public void testRule12_SIFA_Escalation() {
        Warning w = new Warning(WarningType.SIFA, "Test");
        w.setEventTime(new Date(clock.getCurrentTime()));
        kSession.insert(w);
        kSession.fireAllRules();
        
        assertNull(findFact(EmergencyBrakeCommand.class));

        clock.advanceTime(5, TimeUnit.SECONDS);
        kSession.fireAllRules();
        
        EmergencyBrakeCommand ebc = findFact(EmergencyBrakeCommand.class);
        assertNotNull("Emergency brake should be triggered after 5s of unacknowledged SIFA warning", ebc);
    }
    
    @Test
    public void testRule13_Overspeed_ServiceBrake() {
        Warning w = new Warning(WarningType.OVERSPEED, "Overspeed");
        w.setEventTime(new Date(clock.getCurrentTime()));
        kSession.insert(w);
        kSession.fireAllRules();
        
        assertNull(findFact(ServiceBrakeCommand.class));

        clock.advanceTime(3, TimeUnit.SECONDS);
        kSession.fireAllRules();
        
        ServiceBrakeCommand sbc = findFact(ServiceBrakeCommand.class);
        assertNotNull("Service brake should be triggered after 3s of unacknowledged overspeed warning", sbc);
    }
    
    @Test
    public void testRule14b_ClearWheelSlipWarning() {
        Warning w = new Warning(WarningType.WHEEL_SLIP, "Slip");
        w.setEventTime(new Date(clock.getCurrentTime()));
        kSession.insert(w);
        
        WheelSlipEvent slip1 = new WheelSlipEvent();
        slip1.setEventTime(new Date(clock.getCurrentTime()));
        kSession.insert(slip1);
        
        kSession.fireAllRules();
        assertNotNull(findFact(Warning.class));

        clock.advanceTime(5, TimeUnit.SECONDS);
        kSession.fireAllRules();

        assertNull("Wheel slip warning should be cleared when slip subsides for 5s", findFact(Warning.class));
    }
}
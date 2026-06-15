package com.ftn.sbnz.train.service.tests;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

import com.ftn.sbnz.train.model.enums.WarningType;
import com.ftn.sbnz.train.model.events.SandingCommand;
import com.ftn.sbnz.train.model.events.Warning;
import com.ftn.sbnz.train.model.events.WheelSlipEvent;
import com.ftn.sbnz.train.model.facts.BrakingCurve;
import com.ftn.sbnz.train.model.facts.Car;
import com.ftn.sbnz.train.model.facts.Infrastructure;
import com.ftn.sbnz.train.model.facts.MovementAuthority;
import com.ftn.sbnz.train.model.facts.TrainStatus;
import com.ftn.sbnz.train.service.config.DroolsConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TemplatesTest {

    private KieSession kSession;

    @Before
    public void setUp() {
        DroolsConfig config = new DroolsConfig();
        KieContainer container = config.kieContainer();
        kSession = container.newKieSession("trainKsession");
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
    public void testRule2BrakingCurveCargoDry() {
        TrainStatus ts = new TrainStatus();
        ts.setSpeed(50.0);
        ts.setPosition(0.0);
        ts.setTrainType("Cargo");
        
        Infrastructure infra = new Infrastructure();
        infra.setGradient(0.0);
        infra.setWeather("Dry");
        
        MovementAuthority ma = new MovementAuthority();
        ma.setEoa(1000.0);
        
        BrakingCurve bc = new BrakingCurve();
        
        Car loco = new Car("L1", true, 90000, 100);
        Car wagon = new Car("W1", false, 40000, 80);
        
        kSession.insert(ts);
        kSession.insert(infra);
        kSession.insert(ma);
        kSession.insert(bc);
        kSession.insert(loco);
        kSession.insert(wagon);
        
        int fired = kSession.fireAllRules();
        assertTrue("At least one rule should fire", fired > 0);
        
        BrakingCurve updatedBc = findFact(BrakingCurve.class);
        assertNotNull(updatedBc);
        assertTrue("Deceleration should be updated", updatedBc.getEffectiveDeceleration() > 0);
    }
    
    @Test
    public void testRule14WheelSlipDetection() {
        TrainStatus ts = new TrainStatus();
        ts.setSpeed(100.0);
        ts.setWheelSpeed(10.0);
        ts.setTrainType("Passenger");
        
        Infrastructure infra = new Infrastructure();
        infra.setAdhesion(0.20);
        infra.setWeather("Wet");
        
        kSession.insert(ts);
        kSession.insert(infra);

        kSession.insert(new WheelSlipEvent());
        kSession.insert(new WheelSlipEvent());
        kSession.insert(new WheelSlipEvent());
        
        int fired = kSession.fireAllRules();
        assertTrue("Rule 14 should fire", fired > 0);
        
        boolean foundWheelSlip = false;
        for (FactHandle h : kSession.getFactHandles(o -> o instanceof Warning)) {
            Warning warning = (Warning) kSession.getObject(h);
            if (warning.getType() == WarningType.WHEEL_SLIP) {
                foundWheelSlip = true;
                break;
            }
        }
        assertTrue("Should insert wheel slip warning", foundWheelSlip);
        
        SandingCommand sc = findFact(SandingCommand.class);
        assertNotNull("Should insert sanding command", sc);
        
        Infrastructure updatedInfra = findFact(Infrastructure.class);
        assertTrue("Adhesion should be reduced", updatedInfra.getAdhesion() < 0.20);
    }
}

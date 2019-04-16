/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.run;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */
public class RunBerlinScenarioTest {
	private static final Logger log = Logger.getLogger( RunBerlinScenarioTest.class ) ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public final void testTest() {
		// a dummy test to satisfy the matrix build by travis.
		log.info( "Hello world." );
		Assert.assertTrue( true );
	}
	
	// 1pct, testing the scores in iteration 0 and 1
	@Test
	public final void test1person1iteration() {
		try {
			String configFilename = "scenarios/berlin-v5.3-1pct/input/berlin-v5.3-1pct.config.xml";
			RunBerlinScenario berlin = new RunBerlinScenario( new String[] { "--" + RunBerlinScenario.CONFIG_PATH , configFilename } ) ;
			
			Config config =  berlin.prepareConfig();
			config.controler().setLastIteration(0);
			config.strategy().setFractionOfIterationsToDisableInnovation(0);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			config.plans().setInputFile("../../../test/input/test-agents.xml");
			
			Scenario scenario = berlin.prepareScenario();
			Controler controler = berlin.prepareControler();

			TeleportationSpeedEventHandler handler = new TeleportationSpeedEventHandler();

			controler.addOverridingModule(new AbstractModule() {
				
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(handler);
				}
			});
			
			berlin.run();
			
			Assert.assertEquals("Change in score (ride + walk agent)", 128.2797261151769, scenario.getPopulation().getPersons().get(Id.createPersonId("100087501")).getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Change in score (bicycle agent)", 129.80394930541985, scenario.getPopulation().getPersons().get(Id.createPersonId("100200201")).getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Change in score (ride agent)", 131.71443152316658, scenario.getPopulation().getPersons().get(Id.createPersonId("10099501")).getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Change in score (pt agent)", 134.91804284998503, scenario.getPopulation().getPersons().get(Id.createPersonId("100024301")).getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
			
			// look at a single car access_walk trip
			
			final double distance = handler.getPersonId2teleportationDistances().get(Id.createPersonId("100274201")).get(0);
			final double accesswalkSpeedFromConfig = config.plansCalcRoute().getModeRoutingParams().get("access_walk").getTeleportedModeSpeed();
			final double accesswalkSpeedFromEvent = (distance / config.plansCalcRoute().getModeRoutingParams().get("access_walk").getBeelineDistanceFactor()) / handler.getPersonId2teleportationTT().get(Id.createPersonId("100274201")).get(0);
			
			log.warn("distance: " + distance);
			log.warn("access walk teleported mode speed from config: " + accesswalkSpeedFromConfig);
			log.warn("access walk teleported mode speed from event: " + accesswalkSpeedFromEvent);
			
			// previous version (12.0-2019w14-SNAPSHOT):
//			Assert.assertEquals("Change in teleportation speed (car agent: 100274201)", 2.00983175, accesswalkSpeedFromEvent, MatsimTestUtils.EPSILON);			
//			Assert.assertEquals("Change in score (car agent)", 114.88050431935696, scenario.getPopulation().getPersons().get(Id.createPersonId("100274201")).getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
			
			// new version: 
			Assert.assertEquals("Change in teleportation speed (car agent: 100274201)", accesswalkSpeedFromConfig, accesswalkSpeedFromEvent, 0.01);

		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}
	
	// 1pct, testing the scores in iteration 0 and 1
	@Test
	public final void test1pctUntilIteration1() {
		try {
			String configFilename = "scenarios/berlin-v5.3-1pct/input/berlin-v5.3-1pct.config.xml";
			RunBerlinScenario berlin = new RunBerlinScenario( new String[] { "--" + RunBerlinScenario.CONFIG_PATH , configFilename } ) ;
			
			Config config =  berlin.prepareConfig() ;
			config.controler().setLastIteration(1);
			config.strategy().setFractionOfIterationsToDisableInnovation(1);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			
			berlin.run() ;
			
			// Compare with: https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/
			
			Assert.assertEquals("The scores in iteration 0 differ from https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 115.072273500216, berlin.getScoreStats().getScoreHistory().get(ScoreItem.average).get(0), MatsimTestUtils.EPSILON);
			Assert.assertEquals("The scores in iteration 0 differ from https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 115.072273500216, berlin.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), MatsimTestUtils.EPSILON);

			Assert.assertEquals("The scores in iteration 1 differ from https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 114.125389832973, berlin.getScoreStats().getScoreHistory().get(ScoreItem.average).get(1), 0.001);
			Assert.assertEquals("The scores in iteration 1 differ from https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 113.181180406316, berlin.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(1), 0.001);

			// The differences in the scores compared to the run in the public-svn are probably related to the pt raptor router
			// which seems to produce slightly different results (e.g. in case two routes are identical).
			// Thus the large epsilon. ihab, dec'18
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}
	
	// 10pct, testing the scores in iteration 0 and 1
	@Test
	public final void test10pctUntilIteration1() {
		try {
			String configFilename = "scenarios/berlin-v5.3-10pct/input/berlin-v5.3-10pct.config.xml";
			RunBerlinScenario berlin = new RunBerlinScenario( new String[] { "--" + RunBerlinScenario.CONFIG_PATH , configFilename } ) ;
			
			Config config =  berlin.prepareConfig() ;
			config.controler().setLastIteration(1);
			config.strategy().setFractionOfIterationsToDisableInnovation(1);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			
			berlin.run() ;
			
			Assert.assertEquals("The scores in iteration 0 differ from https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-10pct/.", 115.866073407524, berlin.getScoreStats().getScoreHistory().get(ScoreItem.average).get(0), MatsimTestUtils.EPSILON);
			Assert.assertEquals("The scores in iteration 1 differ from https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-10pct/.", 115.02251116746, berlin.getScoreStats().getScoreHistory().get(ScoreItem.average).get(1), 0.001);
			
			// The differences in the scores compared to the run in the public-svn are probably related to the pt raptor router
			// which seems to produce slightly different results (e.g. in case two routes are identical).
			// Thus the large epsilon. ihab, dec'18

		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}
	
	// 1pct, testing the score and modal split in iteration 0 and 40.
	@Test
	public final void test1pctManyIterations() {
		// (right now cannot run even up to 50 iterations because logfile becomes to long.  I can tell maven to send the output
		// to file, but then we don't see anything.  So we will have to play around with the number of iterations.  Thus the
		// imprecise name of the test.   kai, aug'18)
		
		final int iteration = 40;
		try {
			String configFilename = "scenarios/berlin-v5.3-1pct/input/berlin-v5.3-1pct.config.xml";
			RunBerlinScenario berlin = new RunBerlinScenario( new String[] { "--" + RunBerlinScenario.CONFIG_PATH , configFilename } ) ;
			
			Config config = berlin.prepareConfig() ;
			config.controler().setLastIteration(iteration);
//			config.qsim().setEndTime(30 * 3600.);

			config.qsim().setNumberOfThreads( 1 );
			config.global().setNumberOfThreads( 1 );
			// small number of threads in hope to consume less memory.  kai, jul'18
			
			config.strategy().setFractionOfIterationsToDisableInnovation( 1.0 );
			
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			
			config.controler().setWriteEventsInterval( config.controler().getLastIteration() );
			config.controler().setWritePlansUntilIteration( 0 );
			config.controler().setWritePlansInterval( 0 );
			
//			Scenario scenario = berlin.prepareScenario() ;
//			final double sample = 0.1;
//			downsample( scenario.getPopulation().getPersons(), sample ) ;
//			config.qsim().setFlowCapFactor( config.qsim().getFlowCapFactor()*sample );
//			config.qsim().setStorageCapFactor( config.qsim().getStorageCapFactor()*sample );
			
			berlin.run() ;
			
			Gbl.assertNotNull( berlin.getScoreStats() );
			Gbl.assertNotNull( berlin.getScoreStats().getScoreHistory() );			
			Gbl.assertNotNull( berlin.getScoreStats().getScoreHistory().get( ScoreItem.average) );
			Gbl.assertNotNull( berlin.getScoreStats().getScoreHistory().get( ScoreItem.average).get(0) );
			
			Assert.assertEquals("The scores in iteration 0 differ from https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 115.072273500216, berlin.getScoreStats().getScoreHistory().get(ScoreItem.average).get(0), MatsimTestUtils.EPSILON);
			Assert.assertEquals("The scores in iteration 0 differ from https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 115.072273500216, berlin.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), MatsimTestUtils.EPSILON);

			Gbl.assertNotNull( berlin.getScoreStats().getScoreHistory().get( ScoreItem.average).get(iteration) );
			Assert.assertEquals("Major change in the avg. AVG score in iteration " + iteration + " compared to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 114.125389832973, berlin.getScoreStats().getScoreHistory().get(ScoreItem.average).get(iteration), 0.001);
			Assert.assertEquals("Major change in the avg. AVG score in iteration " + iteration + " compared to https://https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 113.181180406316, berlin.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(1), 0.001);

			Map<String,Double> modeCnt = analyzeModeStats(berlin.getPopulation());
			
			double sum = 0 ;
			for ( Double val : modeCnt.values() ) {
				sum += val ;
			}
			
			Assert.assertEquals("Major change in the car trip share compared to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 0.339940203527443, modeCnt.get("car") / sum, 0.01);
			Assert.assertEquals("Major change in the pt trip share compared to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 0.218322955810955, modeCnt.get("pt") / sum, 0.01);
			Assert.assertEquals("Major change in the bicycle trip share compared to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 0.188655127958965, modeCnt.get("bicycle") / sum, 0.01);
			Assert.assertEquals("Major change in the walk trip share compared to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 0.160435581644128, modeCnt.get("walk") / sum, 0.01);
			Assert.assertEquals("Change in the freight trip share compared to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 0.00146473928189373, modeCnt.get("freight") / sum, MatsimTestUtils.EPSILON);
			Assert.assertEquals("Change in the ride trip share compared to https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/.", 0.0911813917766135, modeCnt.get("ride") / sum, MatsimTestUtils.EPSILON);		
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}
	
	private static Map<String, Double> analyzeModeStats(Population population) {
		
		Map<String,Double> modeCnt = new TreeMap<>() ;

		StageActivityTypesImpl stageActivities = new StageActivityTypesImpl(Arrays.asList("pt interaction", "car interaction", "ride interaction", "bicycle interaction", "freight interaction"));
		MainModeIdentifierImpl mainModeIdentifier = new MainModeIdentifierImpl();
		
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan() ;

			List<Trip> trips = TripStructureUtils.getTrips(plan, stageActivities) ;
			for ( Trip trip : trips ) {
				String mode = mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ;
				
				Double cnt = modeCnt.get( mode );
				if ( cnt==null ) {
					cnt = 0. ;
				}
				modeCnt.put( mode, cnt + 1 ) ;
			}
		}

		Logger.getLogger(modeCnt.toString()) ;			
		return modeCnt;	
	}
	
	private static void downsample( final Map map, final double sample ) {
		final Random rnd = MatsimRandom.getLocalInstance();
		log.warn( "map size before=" + map.size() ) ;
		map.values().removeIf( person -> rnd.nextDouble()>sample ) ;
		log.warn( "map size after=" + map.size() ) ;
	}
}

/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.illustrative.parallel.createInput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerSignalController;
import org.matsim.contrib.signals.controller.sylvia.SylviaPreprocessData;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsWriter20;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsWriter20;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LanesToLinkAssignment;

import signals.laemmerFlex.FullyAdaptiveLaemmerSignalController;

/**
 * Class to create signals (signal systems, signal groups and signal control)
 * for the Parallel scenario.
 *
 * @author gthunig, tthunig
 */
public final class TtCreateParallelSignals {

    private static final Logger log = Logger
            .getLogger(TtCreateParallelSignals.class);

    private static final int CYCLE_TIME = 60;
    private static final int INTERGREEN_TIME = 3;
    private static final int MIN_G = 1;
    
    private SignalGroupType groupTypeBranchingNodes = SignalGroupType.SINGLE_GROUPS;
    private SignalGroupType groupTypeInnerNodes = SignalGroupType.SINGLE_GROUPS;
    public enum SignalGroupType {
		SINGLE_GROUPS,
		COMBINE_OUTER_TRAFFIC, // only for branching nodes
		COMBINE_ONCOMING_TRAFFIC,
		ALL_IN_ONE_GROUP // only for branching nodes
    }
    
    private SignalControlType controlTypeBranchingNodes = SignalControlType.FIXED_ALL_GREEN;
    private SignalControlType controlTypeInnerNodes = SignalControlType.FIXED_ALL_OD_COMBINE_HALF_HALF_OFFSET_OPT_ALL_RIGHT;
    public enum SignalControlType {
		FIXED_ALL_GREEN,
		FIXED_ALL_GREEN_OFFSET_OPT_ALL_RIGHT,
		FIXED_ONLY_EW_ALL_RIGHT,
		FIXED_ONLY_EW_HALF_HALF,
		FIXED_ONLY_EW_HALF_HALF_REVERSE,
		FIXED_ALL_OD_CONFLICTING_EQUALLY, // 15:15:15:15
		FIXED_ALL_OD_CONFLICTING_ALL_RIGHT,
		FIXED_ALL_OD_COMBINE_ALL_N_W,
		FIXED_ALL_OD_COMBINE_HALF_HALF,
		FIXED_ALL_OD_COMBINE_HALF_HALF_OFFSET_OPT_ALL_RIGHT, // optimal offsets for link travel time 15sec
		FIXED_PULK_ALL_OD_OFFSET_OPT, // only for branching nodes: 0..27 green
		FIXED_PULK_ONLY_EW_OFFSET_OPT, // only for branching nodes: pulks only in EW direction, all green in NS direction
		LAEMMER_WITH_GROUPS,
		LAEMMER_FLEX
    }
    private boolean useSylviaAtInnerNodes = false;
    private boolean useSylviaAtBranchingNodes = false;

    private Scenario scenario;

    private Map<Id<Link>, List<Id<Link>>> possibleSignalMoves = new HashMap<>();
    private Set<Id<SignalGroup>> signalGroupsFirstODPair = new HashSet<>();
    private Set<Id<SignalGroup>> signalGroupsAllRight = new HashSet<>();
    private Set<Id<SignalGroup>> signalGroupsAllNW = new HashSet<>();
    private Set<Id<SignalGroup>> signalGroupsOutgoing = new HashSet<>();
	private Set<Id<SignalSystem>> branchingSystems = new HashSet<>();
    
    public TtCreateParallelSignals(Scenario scenario) {
        this.scenario = scenario;
    }

    public void createSignals(SignalControlType signalControlBranchingNodes, SignalControlType signalControlInnerNodes, 
    			SignalGroupType signalGroupsBranchingNodes, SignalGroupType singleGroupsInnerNodes) {
        
    		log.info("Create signals ...");
        
    		this.controlTypeBranchingNodes = signalControlBranchingNodes;
        this.controlTypeInnerNodes = signalControlInnerNodes;
        this.groupTypeBranchingNodes = signalGroupsBranchingNodes;
        this.groupTypeInnerNodes = singleGroupsInnerNodes;

        initPossibleSignalMoves();
        prepareSignalControlInfo();

        createSignalSystems();
        createSignalGroups();
        createSignalControlData();
    }
    
    public void setUseSylviaAtInnerNodes(boolean useSylvia) {
    		this.useSylviaAtInnerNodes = useSylvia;
    }
    
    public void setUseSylviaAtBranchingNodes(boolean useSylvia) {
		this.useSylviaAtBranchingNodes = useSylvia;
    }

    private void initPossibleSignalMoves() {

        //signals at node 2
        possibleSignalMoves.put(Id.createLinkId("3_2"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("2_1"))));
        possibleSignalMoves.put(Id.createLinkId("7_2"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("2_1"))));
        possibleSignalMoves.put(Id.createLinkId("1_2"),
                new ArrayList<>(Arrays.asList(Id.createLinkId("2_3"), Id.createLinkId("2_7"))));

        //signals at node 5
        possibleSignalMoves.put(Id.createLinkId("8_5"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("5_6"))));
        possibleSignalMoves.put(Id.createLinkId("4_5"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("5_6"))));
        possibleSignalMoves.put(Id.createLinkId("6_5"),
                new ArrayList<>(Arrays.asList(Id.createLinkId("5_4"), Id.createLinkId("5_8"))));

        //signals at node 10
        possibleSignalMoves.put(Id.createLinkId("3_10"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("10_9"))));
        possibleSignalMoves.put(Id.createLinkId("4_10"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("10_9"))));
        possibleSignalMoves.put(Id.createLinkId("9_10"),
                new ArrayList<>(Arrays.asList(Id.createLinkId("10_3"), Id.createLinkId("10_4"))));

        //signals at node 11
        possibleSignalMoves.put(Id.createLinkId("7_11"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("11_12"))));
        possibleSignalMoves.put(Id.createLinkId("8_11"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("11_12"))));
        // TODO
        possibleSignalMoves.put(Id.createLinkId("12_11"),
                new ArrayList<>(Arrays.asList(Id.createLinkId("11_7"), Id.createLinkId("11_8"))));

        //signals at node 3
        possibleSignalMoves.put(Id.createLinkId("2_3"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("3_4"))));
        possibleSignalMoves.put(Id.createLinkId("10_3"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("3_7"))));
        possibleSignalMoves.put(Id.createLinkId("4_3"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("3_2"))));
        possibleSignalMoves.put(Id.createLinkId("7_3"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("3_10"))));

        //signals at node 4
        possibleSignalMoves.put(Id.createLinkId("3_4"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("4_5"))));
        possibleSignalMoves.put(Id.createLinkId("10_4"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("4_8"))));
        possibleSignalMoves.put(Id.createLinkId("5_4"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("4_3"))));
        possibleSignalMoves.put(Id.createLinkId("8_4"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("4_10"))));

        //signals at node 7
        possibleSignalMoves.put(Id.createLinkId("2_7"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("7_8"))));
        possibleSignalMoves.put(Id.createLinkId("3_7"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("7_11"))));
        possibleSignalMoves.put(Id.createLinkId("11_7"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("7_3"))));
        possibleSignalMoves.put(Id.createLinkId("8_7"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("7_2"))));

        //signals at node 8
        possibleSignalMoves.put(Id.createLinkId("4_8"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("8_11"))));
        possibleSignalMoves.put(Id.createLinkId("5_8"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("8_7"))));
        possibleSignalMoves.put(Id.createLinkId("11_8"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("8_4"))));
        possibleSignalMoves.put(Id.createLinkId("7_8"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("8_5"))));
    }

    private void prepareSignalControlInfo() {    	
		signalGroupsFirstODPair.add(Id.create("signal1_2.2_3", SignalGroup.class));
		signalGroupsFirstODPair.add(Id.create("signal1_2.2_7", SignalGroup.class));
		signalGroupsFirstODPair.add(Id.create("signal3_2.2_1", SignalGroup.class));
		signalGroupsFirstODPair.add(Id.create("signal7_2.2_1", SignalGroup.class));

		signalGroupsFirstODPair.add(Id.create("signal6_5.5_4", SignalGroup.class));
		signalGroupsFirstODPair.add(Id.create("signal6_5.5_8", SignalGroup.class));
		signalGroupsFirstODPair.add(Id.create("signal4_5.5_6", SignalGroup.class));
		signalGroupsFirstODPair.add(Id.create("signal8_5.5_6", SignalGroup.class));

		signalGroupsFirstODPair.add(Id.create("signal7_8.8_5", SignalGroup.class));
		signalGroupsFirstODPair.add(Id.create("signal5_8.8_7", SignalGroup.class));
		signalGroupsFirstODPair.add(Id.create("signal8_7.7_2", SignalGroup.class));
		signalGroupsFirstODPair.add(Id.create("signal2_7.7_8", SignalGroup.class));

		signalGroupsFirstODPair.add(Id.create("signal5_4.4_3", SignalGroup.class));
		signalGroupsFirstODPair.add(Id.create("signal3_4.4_5", SignalGroup.class));
		signalGroupsFirstODPair.add(Id.create("signal4_3.3_2", SignalGroup.class));
		signalGroupsFirstODPair.add(Id.create("signal2_3.3_4", SignalGroup.class));
		
		signalGroupsAllRight.add(Id.create("signal1_2.2_7", SignalGroup.class));
		signalGroupsAllRight.add(Id.create("signal2_7.7_8", SignalGroup.class));
		signalGroupsAllRight.add(Id.create("signal7_8.8_5", SignalGroup.class));
		signalGroupsAllRight.add(Id.create("signal8_5.5_6", SignalGroup.class));
		
		signalGroupsAllRight.add(Id.create("signal6_5.5_4", SignalGroup.class));
		signalGroupsAllRight.add(Id.create("signal5_4.4_3", SignalGroup.class));
		signalGroupsAllRight.add(Id.create("signal4_3.3_2", SignalGroup.class));
		signalGroupsAllRight.add(Id.create("signal3_2.2_1", SignalGroup.class));
		
		signalGroupsAllRight.add(Id.create("signal9_10.10_3", SignalGroup.class));
		signalGroupsAllRight.add(Id.create("signal10_3.3_7", SignalGroup.class));
		signalGroupsAllRight.add(Id.create("signal3_7.7_11", SignalGroup.class));
		signalGroupsAllRight.add(Id.create("signal7_11.11_12", SignalGroup.class));
		
		signalGroupsAllRight.add(Id.create("signal12_11.11_7", SignalGroup.class));
		signalGroupsAllRight.add(Id.create("signal11_8.8_4", SignalGroup.class));
		signalGroupsAllRight.add(Id.create("signal8_4.4_10", SignalGroup.class));
		signalGroupsAllRight.add(Id.create("signal4_10.10_9", SignalGroup.class));
		
		// TODO 'all nw' so far without branching points. add them here if you need them
		signalGroupsAllNW.add(Id.create("signal2_3.3_4", SignalGroup.class));
		signalGroupsAllNW.add(Id.create("signal4_3.3_2", SignalGroup.class));
		signalGroupsAllNW.add(Id.create("signal3_4.4_5", SignalGroup.class));
		signalGroupsAllNW.add(Id.create("signal5_4.4_3", SignalGroup.class));
		
		signalGroupsAllNW.add(Id.create("signal11_7.7_3", SignalGroup.class));
		signalGroupsAllNW.add(Id.create("signal3_7.7_11", SignalGroup.class));
		signalGroupsAllNW.add(Id.create("signal7_3.3_10", SignalGroup.class));
		signalGroupsAllNW.add(Id.create("signal10_3.3_7", SignalGroup.class));
		
        Id<SignalSystem> idSystem2 = Id.create("signalSystem2", SignalSystem.class);
        Id<SignalSystem> idSystem5 = Id.create("signalSystem5", SignalSystem.class);
        Id<SignalSystem> idSystem10 = Id.create("signalSystem10", SignalSystem.class);
        Id<SignalSystem> idSystem11 = Id.create("signalSystem11", SignalSystem.class);
		branchingSystems.add(idSystem2);
		branchingSystems.add(idSystem5);
		branchingSystems.add(idSystem10);
		branchingSystems.add(idSystem11);
		
		signalGroupsOutgoing.add(Id.create("signal3_2.2_1", SignalGroup.class));
		signalGroupsOutgoing.add(Id.create("signal7_2.2_1", SignalGroup.class));
		signalGroupsOutgoing.add(Id.create("signal7_11.11_12", SignalGroup.class));
		signalGroupsOutgoing.add(Id.create("signal8_11.11_12", SignalGroup.class));
		signalGroupsOutgoing.add(Id.create("signal8_5.5_6", SignalGroup.class));
		signalGroupsOutgoing.add(Id.create("signal4_5.5_6", SignalGroup.class));
		signalGroupsOutgoing.add(Id.create("signal4_10.10_9", SignalGroup.class));
		signalGroupsOutgoing.add(Id.create("signal3_10.10_9", SignalGroup.class));
	}

    /**
     * Creates signal systems depending on the network situation.
     */
    private void createSignalSystems() {

        createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(2)));
        createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(3)));
        createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(4)));
        createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(5)));
        createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(7)));
        createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(8)));

        if (TtCreateParallelNetworkAndLanes.checkNetworkForSecondODPair(this.scenario.getNetwork())) {
            createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(10)));
            createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(11)));
        }
    }

    private void createSignalSystemAtNode(Node node) {
		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
        SignalSystemsDataFactory fac = signalSystems.getFactory(); 

        // create signal system
		SignalSystemData signalSystem = fac.createSignalSystemData(Id.create("signalSystem" + node.getId(), SignalSystem.class));
        signalSystems.addSignalSystemData(signalSystem);

        // create a signal for every inLink outLink pair that is contained in possibleSignalMoves
        for (Id<Link> inLinkId : node.getInLinks().keySet()) {
            for (Id<Link> outLinkId : node.getOutLinks().keySet()) {
				if (possibleSignalMoves.containsKey(inLinkId) && possibleSignalMoves.get(inLinkId).contains(outLinkId)) {

					SignalData signal = fac.createSignalData(Id.create("signal" + inLinkId + "." + outLinkId, Signal.class));
                    signal.setLinkId(inLinkId);
                    signal.addTurningMoveRestriction(outLinkId);

                    LanesToLinkAssignment linkLanes = this.scenario.getLanes().getLanesToLinkAssignments().get(inLinkId);
                    if (linkLanes != null) {
                        for (Lane l : linkLanes.getLanes().values()) {
                            if (l.getToLinkIds() != null) {
                                for (Id<Link> toLinkId : l.getToLinkIds()) {
                                    if (toLinkId.toString().equals(outLinkId.toString())) {
                                        signal.addLaneId(l.getId());
                                    }
                                }
                            }
                        }

                    }
                    signalSystem.addSignalData(signal);
                }
            }
        }
    }

    private void createSignalGroups() {

		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
        SignalSystemsData signalSystems = signalsData.getSignalSystemsData();

		switch (groupTypeBranchingNodes) {
		case SINGLE_GROUPS:
			for (SignalSystemData system : signalSystems.getSignalSystemData().values()) {
				if (branchingSystems.contains(system.getId()))
					SignalUtils.createAndAddSignalGroups4Signals(signalGroups, system);
			}
			break;
		case COMBINE_OUTER_TRAFFIC:
			createGroupsForBranchPoints(signalGroups);
			break;
		case COMBINE_ONCOMING_TRAFFIC:
			createOncomingGroupsForBranchPoints(signalGroups);
			break;
		case ALL_IN_ONE_GROUP:
			createCommonGroupForBranchPoints(signalGroups);
			break;
		}
		switch (groupTypeInnerNodes) {
		case SINGLE_GROUPS:
			for (SignalSystemData system : signalSystems.getSignalSystemData().values()) {
				if (!branchingSystems.contains(system.getId()))
					SignalUtils.createAndAddSignalGroups4Signals(signalGroups, system);
			}
			break;
		case COMBINE_ONCOMING_TRAFFIC:
			createGroupsForOncomingTrafficAtInnerNodes(signalGroups);
			break;
		default:
			throw new RuntimeException("Signal group " + groupTypeInnerNodes + " does not make sense for inner nodes.");
		}
    }

    // for systems 3,4,7,8
	private void createGroupsForOncomingTrafficAtInnerNodes(SignalGroupsData signalGroups) {
		// create groups for system 3
		Id<SignalSystem> idSystem3 = Id.create("signalSystem3", SignalSystem.class);
		SignalGroupData groupEW_3 = signalGroups.getFactory().createSignalGroupData(idSystem3, 
				Id.create("groupEW_3", SignalGroup.class));
		groupEW_3.addSignalId(Id.create("signal2_3.3_4", Signal.class));
		groupEW_3.addSignalId(Id.create("signal4_3.3_2", Signal.class));
		signalGroups.addSignalGroupData(groupEW_3);
		
		// create groups for system 4
		Id<SignalSystem> idSystem4 = Id.create("signalSystem4", SignalSystem.class);
		SignalGroupData groupEW_4 = signalGroups.getFactory().createSignalGroupData(idSystem4,
				Id.create("groupEW_4", SignalGroup.class));
		groupEW_4.addSignalId(Id.create("signal3_4.4_5", Signal.class));
		groupEW_4.addSignalId(Id.create("signal5_4.4_3", Signal.class));
		signalGroups.addSignalGroupData(groupEW_4);
		
		// create groups for system 7
		Id<SignalSystem> idSystem7 = Id.create("signalSystem7", SignalSystem.class);
		SignalGroupData groupEW_7 = signalGroups.getFactory().createSignalGroupData(idSystem7,
				Id.create("groupEW_7", SignalGroup.class));
		groupEW_7.addSignalId(Id.create("signal2_7.7_8", Signal.class));
		groupEW_7.addSignalId(Id.create("signal8_7.7_2", Signal.class));
		signalGroups.addSignalGroupData(groupEW_7);
		
		// create groups for system 8
		Id<SignalSystem> idSystem8 = Id.create("signalSystem8", SignalSystem.class);
		SignalGroupData groupEW_8 = signalGroups.getFactory().createSignalGroupData(idSystem8,
				Id.create("groupEW_8", SignalGroup.class));
		groupEW_8.addSignalId(Id.create("signal7_8.8_5", Signal.class));
		groupEW_8.addSignalId(Id.create("signal5_8.8_7", Signal.class));
		signalGroups.addSignalGroupData(groupEW_8);
		
		if (TtCreateParallelNetworkAndLanes.checkNetworkForSecondODPair(this.scenario.getNetwork())) {
			SignalGroupData groupNS_3 = signalGroups.getFactory().createSignalGroupData(idSystem3,
					Id.create("groupNS_3", SignalGroup.class));
			groupNS_3.addSignalId(Id.create("signal10_3.3_7", Signal.class));
			groupNS_3.addSignalId(Id.create("signal7_3.3_10", Signal.class));
			signalGroups.addSignalGroupData(groupNS_3);
			
			SignalGroupData groupNS_4 = signalGroups.getFactory().createSignalGroupData(idSystem4,
					Id.create("groupNS_4", SignalGroup.class));
			groupNS_4.addSignalId(Id.create("signal10_4.4_8", Signal.class));
			groupNS_4.addSignalId(Id.create("signal8_4.4_10", Signal.class));
			signalGroups.addSignalGroupData(groupNS_4);
			
			SignalGroupData groupNS_7 = signalGroups.getFactory().createSignalGroupData(idSystem7,
					Id.create("groupNS_7", SignalGroup.class));
			groupNS_7.addSignalId(Id.create("signal3_7.7_11", Signal.class));
			groupNS_7.addSignalId(Id.create("signal11_7.7_3", Signal.class));
			signalGroups.addSignalGroupData(groupNS_7);
			
			SignalGroupData groupNS_8 = signalGroups.getFactory().createSignalGroupData(idSystem8,
					Id.create("groupNS_8", SignalGroup.class));
			groupNS_8.addSignalId(Id.create("signal4_8.8_11", Signal.class));
			groupNS_8.addSignalId(Id.create("signal11_8.8_4", Signal.class));
			signalGroups.addSignalGroupData(groupNS_8);
		}
	}

	private void createCommonGroupForBranchPoints(SignalGroupsData signalGroups) {
		// create groups for system 2
        SignalGroupData group2 = signalGroups.getFactory().createSignalGroupData(Id.create("signalSystem2", SignalSystem.class), 
				Id.create("signal2", SignalGroup.class));
		group2.addSignalId(Id.create("signal1_2.2_3", Signal.class));
		group2.addSignalId(Id.create("signal7_2.2_1", Signal.class));
		group2.addSignalId(Id.create("signal1_2.2_7", Signal.class));
		group2.addSignalId(Id.create("signal3_2.2_1", Signal.class));
		signalGroups.addSignalGroupData(group2);
		
		// create groups for system 5
		SignalGroupData group5 = signalGroups.getFactory().createSignalGroupData(Id.create("signalSystem5", SignalSystem.class), 
				Id.create("signal5", SignalGroup.class));
		group5.addSignalId(Id.create("signal6_5.5_8", Signal.class));
		group5.addSignalId(Id.create("signal4_5.5_6", Signal.class));
		group5.addSignalId(Id.create("signal6_5.5_4", Signal.class));
		group5.addSignalId(Id.create("signal8_5.5_6", Signal.class));
		signalGroups.addSignalGroupData(group5);
		
		if (TtCreateParallelNetworkAndLanes.checkNetworkForSecondODPair(this.scenario.getNetwork())) {
			// create groups for system 10
			SignalGroupData group10 = signalGroups.getFactory().createSignalGroupData(Id.create("signalSystem10", SignalSystem.class), 
					Id.create("signal10", SignalGroup.class));
			group10.addSignalId(Id.create("signal9_10.10_4", Signal.class));
			group10.addSignalId(Id.create("signal3_10.10_9", Signal.class));
			group10.addSignalId(Id.create("signal9_10.10_3", Signal.class));
			group10.addSignalId(Id.create("signal4_10.10_9", Signal.class));
			signalGroups.addSignalGroupData(group10);

			// create groups for system 11
			SignalGroupData group11 = signalGroups.getFactory().createSignalGroupData(Id.create("signalSystem11", SignalSystem.class), 
					Id.create("signal11", SignalGroup.class));
			group11.addSignalId(Id.create("signal12_11.11_7", Signal.class));
			group11.addSignalId(Id.create("signal8_11.11_12", Signal.class));
			group11.addSignalId(Id.create("signal12_11.11_8", Signal.class));
			group11.addSignalId(Id.create("signal7_11.11_12", Signal.class));
			signalGroups.addSignalGroupData(group11);
		}
	}

	private void createGroupsForBranchPoints(SignalGroupsData signalGroups) {
		// create groups for system 2
        Id<SignalSystem> idSystem2 = Id.create("signalSystem2", SignalSystem.class);
		SignalGroupData groupLeftTurn12 = signalGroups.getFactory().createSignalGroupData(idSystem2, 
				Id.create("signalLeftTurn1_2", SignalGroup.class));
		groupLeftTurn12.addSignalId(Id.create("signal1_2.2_3", Signal.class));
		signalGroups.addSignalGroupData(groupLeftTurn12);

		SignalGroupData groupLeftTurn72 = signalGroups.getFactory().createSignalGroupData(idSystem2, 
				Id.create("signalLeftTurn7_2", SignalGroup.class));
		groupLeftTurn72.addSignalId(Id.create("signal7_2.2_1", Signal.class));
		signalGroups.addSignalGroupData(groupLeftTurn72);
		
		SignalGroupData groupRightTurns1232 = signalGroups.getFactory().createSignalGroupData(idSystem2, 
				Id.create("signalRightTurns2", SignalGroup.class));
		groupRightTurns1232.addSignalId(Id.create("signal1_2.2_7", Signal.class));
		groupRightTurns1232.addSignalId(Id.create("signal3_2.2_1", Signal.class));
		signalGroups.addSignalGroupData(groupRightTurns1232);
		
		// create groups for system 5
		Id<SignalSystem> idSystem5 = Id.create("signalSystem5", SignalSystem.class);
		SignalGroupData groupLeftTurn65 = signalGroups.getFactory().createSignalGroupData(idSystem5, 
				Id.create("signalLeftTurn6_5", SignalGroup.class));
		groupLeftTurn65.addSignalId(Id.create("signal6_5.5_8", Signal.class));
		signalGroups.addSignalGroupData(groupLeftTurn65);

		SignalGroupData groupLeftTurn45 = signalGroups.getFactory().createSignalGroupData(idSystem5, 
				Id.create("signalLeftTurn4_5", SignalGroup.class));
		groupLeftTurn45.addSignalId(Id.create("signal4_5.5_6", Signal.class));
		signalGroups.addSignalGroupData(groupLeftTurn45);
		
		SignalGroupData groupRightTurns6585 = signalGroups.getFactory().createSignalGroupData(idSystem5, 
				Id.create("signalRightTurns5", SignalGroup.class));
		groupRightTurns6585.addSignalId(Id.create("signal6_5.5_4", Signal.class));
		groupRightTurns6585.addSignalId(Id.create("signal8_5.5_6", Signal.class));
		signalGroups.addSignalGroupData(groupRightTurns6585);
		
		if (TtCreateParallelNetworkAndLanes.checkNetworkForSecondODPair(this.scenario.getNetwork())) {
			// create groups for system 10
			Id<SignalSystem> idSystem10 = Id.create("signalSystem10", SignalSystem.class);
			SignalGroupData groupLeftTurn910 = signalGroups.getFactory().createSignalGroupData(idSystem10, 
					Id.create("signalLeftTurn9_10", SignalGroup.class));
			groupLeftTurn910.addSignalId(Id.create("signal9_10.10_4", Signal.class));
			signalGroups.addSignalGroupData(groupLeftTurn910);

			SignalGroupData groupLeftTurn310 = signalGroups.getFactory().createSignalGroupData(idSystem10, 
					Id.create("signalLeftTurn3_10", SignalGroup.class));
			groupLeftTurn310.addSignalId(Id.create("signal3_10.10_9", Signal.class));
			signalGroups.addSignalGroupData(groupLeftTurn310);
			
			SignalGroupData groupRightTurns910410 = signalGroups.getFactory().createSignalGroupData(idSystem10, 
					Id.create("signalRightTurns10", SignalGroup.class));
			groupRightTurns910410.addSignalId(Id.create("signal9_10.10_3", Signal.class));
			groupRightTurns910410.addSignalId(Id.create("signal4_10.10_9", Signal.class));
			signalGroups.addSignalGroupData(groupRightTurns910410);
			
			// create groups for system 11
			Id<SignalSystem> idSystem11 = Id.create("signalSystem11", SignalSystem.class);
			SignalGroupData groupLeftTurn1211 = signalGroups.getFactory().createSignalGroupData(idSystem11, 
					Id.create("signalLeftTurn12_11", SignalGroup.class));
			groupLeftTurn1211.addSignalId(Id.create("signal12_11.11_7", Signal.class));
			signalGroups.addSignalGroupData(groupLeftTurn1211);

			SignalGroupData groupLeftTurn811 = signalGroups.getFactory().createSignalGroupData(idSystem11, 
					Id.create("signalLeftTurn8_11", SignalGroup.class));
			groupLeftTurn811.addSignalId(Id.create("signal8_11.11_12", Signal.class));
			signalGroups.addSignalGroupData(groupLeftTurn811);
			
			SignalGroupData groupRightTurns1211711 = signalGroups.getFactory().createSignalGroupData(idSystem11, 
					Id.create("signalRightTurns11", SignalGroup.class));
			groupRightTurns1211711.addSignalId(Id.create("signal12_11.11_8", Signal.class));
			groupRightTurns1211711.addSignalId(Id.create("signal7_11.11_12", Signal.class));
			signalGroups.addSignalGroupData(groupRightTurns1211711);
		}
	}
	
	private void createOncomingGroupsForBranchPoints(SignalGroupsData signalGroups) {
		// create groups for system 2
        Id<SignalSystem> idSystem2 = Id.create("signalSystem2", SignalSystem.class);
		SignalGroupData signalIn2 = signalGroups.getFactory().createSignalGroupData(idSystem2, 
				Id.create("signalIn2", SignalGroup.class));
		signalIn2.addSignalId(Id.create("signal1_2.2_3", Signal.class));
		signalIn2.addSignalId(Id.create("signal1_2.2_7", Signal.class));
		signalGroups.addSignalGroupData(signalIn2);

		SignalGroupData signalOut2 = signalGroups.getFactory().createSignalGroupData(idSystem2, 
				Id.create("signalLeftTurn7_2", SignalGroup.class));
		signalOut2.addSignalId(Id.create("signal7_2.2_1", Signal.class));
		signalOut2.addSignalId(Id.create("signal3_2.2_1", Signal.class));
		signalGroups.addSignalGroupData(signalOut2);
		
		// create groups for system 5
		Id<SignalSystem> idSystem5 = Id.create("signalSystem5", SignalSystem.class);
		SignalGroupData signalIn5 = signalGroups.getFactory().createSignalGroupData(idSystem5, 
				Id.create("signalIn5", SignalGroup.class));
		signalIn5.addSignalId(Id.create("signal6_5.5_8", Signal.class));
		signalIn5.addSignalId(Id.create("signal6_5.5_4", Signal.class));
		signalGroups.addSignalGroupData(signalIn5);

		SignalGroupData signalOut5 = signalGroups.getFactory().createSignalGroupData(idSystem5, 
				Id.create("signalOut5", SignalGroup.class));
		signalOut5.addSignalId(Id.create("signal4_5.5_6", Signal.class));
		signalOut5.addSignalId(Id.create("signal8_5.5_6", Signal.class));
		signalGroups.addSignalGroupData(signalOut5);
		
		if (TtCreateParallelNetworkAndLanes.checkNetworkForSecondODPair(this.scenario.getNetwork())) {
			// create groups for system 10
			Id<SignalSystem> idSystem10 = Id.create("signalSystem10", SignalSystem.class);
			SignalGroupData signalIn10 = signalGroups.getFactory().createSignalGroupData(idSystem10, 
					Id.create("signalIn10", SignalGroup.class));
			signalIn10.addSignalId(Id.create("signal9_10.10_4", Signal.class));
			signalIn10.addSignalId(Id.create("signal9_10.10_3", Signal.class));
			signalGroups.addSignalGroupData(signalIn10);

			SignalGroupData signalOut10 = signalGroups.getFactory().createSignalGroupData(idSystem10, 
					Id.create("signalOut10", SignalGroup.class));
			signalOut10.addSignalId(Id.create("signal3_10.10_9", Signal.class));
			signalOut10.addSignalId(Id.create("signal4_10.10_9", Signal.class));
			signalGroups.addSignalGroupData(signalOut10);
			
			// create groups for system 11
			Id<SignalSystem> idSystem11 = Id.create("signalSystem11", SignalSystem.class);
			SignalGroupData signalIn11 = signalGroups.getFactory().createSignalGroupData(idSystem11, 
					Id.create("signalIn11", SignalGroup.class));
			signalIn11.addSignalId(Id.create("signal12_11.11_7", Signal.class));
			signalIn11.addSignalId(Id.create("signal12_11.11_8", Signal.class));
			signalGroups.addSignalGroupData(signalIn11);

			SignalGroupData signalOut11 = signalGroups.getFactory().createSignalGroupData(idSystem11, 
					Id.create("signalOut11", SignalGroup.class));
			signalOut11.addSignalId(Id.create("signal8_11.11_12", Signal.class));
			signalOut11.addSignalId(Id.create("signal7_11.11_12", Signal.class));
			signalGroups.addSignalGroupData(signalOut11);
		}
	}

    private void createSignalControlData() {

		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
        SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
        SignalControlData signalControl = signalsData.getSignalControlData();
        SignalControlDataFactory fac = signalControl.getFactory();
        
		// create a temporary, empty signal control object needed in case sylvia is used
		SignalControlData tmpSignalControl = new SignalControlDataImpl();

        // creates a signal control for all signal systems
		for (SignalSystemData signalSystem : signalSystems.getSignalSystemData().values()) {

			// distinguish between branching and inner nodes
			if (branchingSystems.contains(signalSystem.getId())) {
				// branching node
				
				SignalSystemControllerData signalSystemControl = fac.createSignalSystemControllerData(signalSystem.getId());
				// add the signalSystemControl to the (final or temporary) signalControl
				if (useSylviaAtBranchingNodes) {
					tmpSignalControl.addSignalSystemControllerData(signalSystemControl);
				} else {
					signalControl.addSignalSystemControllerData(signalSystemControl);
				}
				
				switch (controlTypeBranchingNodes) {
				case FIXED_ALL_GREEN:
					SignalPlanData signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
					for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
						createSingleGreenFixedSignalControl(fac, signalPlan, signalGroup.getId(), 60);
					}
					break;
				case FIXED_ALL_GREEN_OFFSET_OPT_ALL_RIGHT:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
					for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
						createSingleGreenFixedSignalControl(fac, signalPlan, signalGroup.getId(), 60);
					}
					// set offset
					switch (signalSystem.getId().toString()) {
					case "signalSystem10":
						signalPlan.setOffset(0 + 30);
						break;
					case "signalSystem2":
						signalPlan.setOffset(15);
						break;
					case "signalSystem11":
						signalPlan.setOffset(30 + 30);
						break;
					case "signalSystem5":
						signalPlan.setOffset(45);
						break;
					}
					break;
				case FIXED_ONLY_EW_ALL_RIGHT:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
		            for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
						createOneOdAllRightSignalControl(fac, signalPlan, signalGroup.getId());
		            }
					break;
				case FIXED_ONLY_EW_HALF_HALF:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
		            for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
		            		createOneOdHalfHalfRightLeftSignalControl(fac, signalPlan, signalGroup.getId());
					}
					break;
				case FIXED_ONLY_EW_HALF_HALF_REVERSE:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
		            for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
		            		createOneOdHalfHalfRightLeftSignalControlReverse(fac, signalPlan, signalGroup.getId());
					}
					break;
				case FIXED_ALL_OD_CONFLICTING_EQUALLY:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
		            for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
						createSecondOdAllConflictingEqualSignalControl(fac, signalPlan, signalGroup.getId());
					}
					break;
				case FIXED_ALL_OD_CONFLICTING_ALL_RIGHT:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
		            for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
						createSecondOdAllRightSignalControl(fac, signalPlan, signalGroup.getId());
					}
		            break;
				case FIXED_ALL_OD_COMBINE_HALF_HALF:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
		            for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
		            		createSecondOd30EachFixedSignalControl(fac, signalPlan, signalGroup.getId());
					}
					break;
				case FIXED_PULK_ALL_OD_OFFSET_OPT:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
		            for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
						if (signalGroupsOutgoing.contains(signalGroup.getId())) {
							createSingleGreenFixedSignalControl(fac, signalPlan, signalGroup.getId(), 60);
						} else { // ingoing 							
							createSingleGreenFixedSignalControl(fac, signalPlan, signalGroup.getId(), 30);
						}
					}
					// set offset
					switch (signalSystem.getId().toString()) {
					case "signalSystem10":
						signalPlan.setOffset(0 + 30);
						break;
					case "signalSystem2":
						signalPlan.setOffset(15);
						break;
					case "signalSystem11":
						signalPlan.setOffset(30 + 30);
						break;
					case "signalSystem5":
						signalPlan.setOffset(45);
						break;
					}
					break;
				case FIXED_PULK_ONLY_EW_OFFSET_OPT:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
		            for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
						if (!signalGroupsFirstODPair.contains(signalGroup.getId()) || signalGroupsOutgoing.contains(signalGroup.getId())) {
							createSingleGreenFixedSignalControl(fac, signalPlan, signalGroup.getId(), 60);
						} else { // ingoing signal group of the first od pair							
							createSingleGreenFixedSignalControl(fac, signalPlan, signalGroup.getId(), 30);
						}
					}
					// set offset
					switch (signalSystem.getId().toString()) {
					case "signalSystem10":
						signalPlan.setOffset(0 + 30);
						break;
					case "signalSystem2":
						signalPlan.setOffset(15);
						break;
					case "signalSystem11":
						signalPlan.setOffset(30 + 30);
						break;
					case "signalSystem5":
						signalPlan.setOffset(45);
						break;
					}
					break;
				case LAEMMER_WITH_GROUPS:
					signalSystemControl.setControllerIdentifier(LaemmerSignalController.IDENTIFIER);
					break;
				case LAEMMER_FLEX:
					signalSystemControl.setControllerIdentifier(FullyAdaptiveLaemmerSignalController.IDENTIFIER);
					// TODO create conflicts
					throw new UnsupportedOperationException();
//					break;
				default:
					throw new RuntimeException("It does not make sense to use " + controlTypeBranchingNodes + " for branching nodes.");
				}
			} 
			// ---------------------------------------------------------------------------------------------------------------------------------------
			else { // inner node
				
				SignalSystemControllerData signalSystemControl = fac.createSignalSystemControllerData(signalSystem.getId());
				// add the signalSystemControl to the (final or temporary) signalControl
				if (useSylviaAtInnerNodes) {
					tmpSignalControl.addSignalSystemControllerData(signalSystemControl);
				} else {
					signalControl.addSignalSystemControllerData(signalSystemControl);
				}
				
				switch (controlTypeInnerNodes) {
				case FIXED_ALL_GREEN:
					SignalPlanData signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
					for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
						createSingleGreenFixedSignalControl(fac, signalPlan, signalGroup.getId(), 60);
					}
					break;
				case FIXED_ONLY_EW_ALL_RIGHT:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
		            for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
						createOneOdAllRightSignalControl(fac, signalPlan, signalGroup.getId());
		            }
					break;
				case FIXED_ONLY_EW_HALF_HALF:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
		            for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
		            		createOneOdHalfHalfRightLeftSignalControl(fac, signalPlan, signalGroup.getId());
					}
					break;
				case FIXED_ONLY_EW_HALF_HALF_REVERSE:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
		            for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
		            		createOneOdHalfHalfRightLeftSignalControlReverse(fac, signalPlan, signalGroup.getId());
					}
					break;
				case FIXED_ALL_OD_CONFLICTING_EQUALLY:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
		            for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
						createSecondOdAllConflictingEqualSignalControl(fac, signalPlan, signalGroup.getId());
					}
					break;
				case FIXED_ALL_OD_CONFLICTING_ALL_RIGHT:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
		            for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
						createSecondOdAllRightSignalControl(fac, signalPlan, signalGroup.getId());
					}
		            break;
				case FIXED_ALL_OD_COMBINE_ALL_N_W:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
		            for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
						createSecondOdAllNWSignalControl(fac, signalPlan, signalGroup.getId(), signalSystem.getId());
					}
		            break;
				case FIXED_ALL_OD_COMBINE_HALF_HALF:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
		            for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
		            		createSecondOd30EachFixedSignalControl(fac, signalPlan, signalGroup.getId());
					}
					break;
				case FIXED_ALL_OD_COMBINE_HALF_HALF_OFFSET_OPT_ALL_RIGHT:
					signalPlan = createBasisFixedTimePlan(fac, signalSystemControl);
		            for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
		            		createSecondOd30EachFixedSignalControl(fac, signalPlan, signalGroup.getId());
					}
		            // set offset
					switch (signalSystem.getId().toString()) {
					case "signalSystem4":
						signalPlan.setOffset(0);
						break;
					case "signalSystem3":
						signalPlan.setOffset(15);
						break;
					case "signalSystem7":
						signalPlan.setOffset(30);
						break;
					case "signalSystem8":
						signalPlan.setOffset(45);
						break;
					}
					break;
				case LAEMMER_WITH_GROUPS:
					signalSystemControl.setControllerIdentifier(LaemmerSignalController.IDENTIFIER);
					break;
				case LAEMMER_FLEX:
					signalSystemControl.setControllerIdentifier(FullyAdaptiveLaemmerSignalController.IDENTIFIER);
					// TODO create conflicts
					throw new UnsupportedOperationException();
//					break;
				default:
					throw new RuntimeException("It does not make sense to use " + controlTypeInnerNodes + " for inner nodes.");
				}
			}
        }
		
		// convert basis fixed time plan to sylvia plan
		if (useSylviaAtBranchingNodes || useSylviaAtInnerNodes) {
			SylviaPreprocessData.convertSignalControlData(tmpSignalControl, signalControl);
		}
    }

	private void createSingleGreenFixedSignalControl(SignalControlDataFactory fac, SignalPlanData signalPlan,
			Id<SignalGroup> id, int greenSeconds) {
		int onset = 0; 
		int dropping = greenSeconds - INTERGREEN_TIME;
		int signalSystemOffset = 0;
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, id, onset, dropping));
		signalPlan.setOffset(signalSystemOffset);
	}

	private void createOneOdHalfHalfRightLeftSignalControl(SignalControlDataFactory fac,
			SignalPlanData signalPlan, Id<SignalGroup> signalGroupId) {
		int onset; 
		int dropping;
		int signalSystemOffset = 0;
		if (signalGroupsAllRight.contains(signalGroupId)) {
			onset = 0;
			dropping = 30 - INTERGREEN_TIME;
		} else {
			onset = 30;
			dropping = 60 - INTERGREEN_TIME;
		}
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, signalGroupId, onset, dropping));
		signalPlan.setOffset(signalSystemOffset);
	}
	
	private void createOneOdHalfHalfRightLeftSignalControlReverse(SignalControlDataFactory fac,
			SignalPlanData signalPlan, Id<SignalGroup> signalGroupId) {
		int onset; 
		int dropping;
		int signalSystemOffset = 0;
		if (signalGroupsAllRight.contains(signalGroupId)) {
			onset = 30;
			dropping = 60 - INTERGREEN_TIME;
		} else {
			onset = 0;
			dropping = 30 - INTERGREEN_TIME;
		}
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, signalGroupId, onset, dropping));
		signalPlan.setOffset(signalSystemOffset);
	}

	private SignalPlanData createBasisFixedTimePlan(SignalControlDataFactory fac,
			SignalSystemControllerData signalSystemControl) {
		if (branchingSystems.contains(signalSystemControl.getSignalSystemId())) {
			if (!groupTypeBranchingNodes.equals(SignalGroupType.SINGLE_GROUPS)) {
				throw new RuntimeException("please select SINGLE_GROUPS when using fixed-time signals");
			}
		} else if (!groupTypeInnerNodes.equals(SignalGroupType.SINGLE_GROUPS)) {
			throw new RuntimeException("please select SINGLE_GROUPS when using fixed-time signals");			
		}
		
		signalSystemControl.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		// create a default plan for the signal system (with defined cycle time and offset 0)
		SignalPlanData signalPlan = SignalUtils.createSignalPlan(fac, CYCLE_TIME, 0);
		signalSystemControl.addSignalPlanData(signalPlan);
		return signalPlan;
	}

	private void createOneOdAllRightSignalControl(SignalControlDataFactory fac, SignalPlanData signalPlan, Id<SignalGroup> signalGroupId) {
		int onset;
		int dropping;
		int signalSystemOffset;
		if (signalGroupsAllRight.contains(signalGroupId)) {
			onset = 0;
			dropping = 60 - 2 * INTERGREEN_TIME - MIN_G;
			signalSystemOffset = 0;
		} else {
			onset = 60 - INTERGREEN_TIME - MIN_G;
			dropping = 60 - INTERGREEN_TIME;
			signalSystemOffset = 0;
		}
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, signalGroupId, onset, dropping));
		signalPlan.setOffset(signalSystemOffset);
	}

	private void createSecondOdAllRightSignalControl(SignalControlDataFactory fac,
			SignalPlanData signalPlan, Id<SignalGroup> signalGroupId) {
		int onset;
		int dropping;
		int signalSystemOffset;
		if (signalGroupsFirstODPair.contains(signalGroupId)) {
			if (signalGroupsAllRight.contains(signalGroupId)) {
				onset = 0;
				dropping = 30 - 2 * INTERGREEN_TIME - MIN_G;
				signalSystemOffset = 0;
			} else {
				onset = 30 - INTERGREEN_TIME - MIN_G;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
			}
		} else {
			if (signalGroupsAllRight.contains(signalGroupId)) {
				onset = 30;
				dropping = 60 - 2 * INTERGREEN_TIME - MIN_G;
				signalSystemOffset = 0;
			} else {
				onset = 60 - INTERGREEN_TIME - MIN_G;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
			}
		}
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, signalGroupId, onset, dropping));
		signalPlan.setOffset(signalSystemOffset);
	}

	private void createSecondOdAllNWSignalControl(SignalControlDataFactory fac, SignalPlanData signalPlan,
			Id<SignalGroup> signalGroupId, Id<SignalSystem> signalSystemId) {
		int onset;
		int dropping;
		int signalSystemOffset;
		if (signalSystemId.equals(Id.create("signalSystem3", SignalSystem.class))) {
			if (signalGroupsAllNW.contains(signalGroupId)) {
				if (signalGroupsFirstODPair.contains(signalGroupId)) {
					onset = 0;
					dropping = 30 - 2 * INTERGREEN_TIME - MIN_G;
					signalSystemOffset = 0;
				} else {
					onset = 30;
					dropping = 60 - 2 * INTERGREEN_TIME - MIN_G;
					signalSystemOffset = 0;
				}
			} else {
				if (signalGroupsFirstODPair.contains(signalGroupId)) {
					onset = 30 - INTERGREEN_TIME - MIN_G;
					dropping = 30 - INTERGREEN_TIME;
					signalSystemOffset = 0;
				} else {
					onset = 60 - INTERGREEN_TIME - MIN_G;
					dropping = 60 - INTERGREEN_TIME;
					signalSystemOffset = 0;
				}
			}
		} else {
			if (signalGroupsAllNW.contains(signalGroupId)) {
				onset = 0;
				dropping = 60 - 2 * INTERGREEN_TIME - MIN_G;
				signalSystemOffset = 0;
			} else {
				onset = 60 - INTERGREEN_TIME - MIN_G;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
			}
		}
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, signalGroupId, onset, dropping));
		signalPlan.setOffset(signalSystemOffset);
	}

	private void createSecondOd30EachFixedSignalControl(SignalControlDataFactory fac, SignalPlanData signalPlan, Id<SignalGroup> signalGroupId) {
		int onset;
		int dropping;
		int signalSystemOffset;
		if (signalGroupsFirstODPair.contains(signalGroupId)) {
			// the signal belongs to the first OD-pair
			onset = 0;
			dropping = 30 - INTERGREEN_TIME;
			signalSystemOffset = 0;
		} else {
			// the signal belongs to the second OD-pair
			onset = 30;
			dropping = 60 - INTERGREEN_TIME;
			signalSystemOffset = 0;
		}
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, signalGroupId, onset, dropping));
		signalPlan.setOffset(signalSystemOffset);
    }

    private void createSecondOdAllConflictingEqualSignalControl(SignalControlDataFactory fac,
			SignalPlanData signalPlan, Id<SignalGroup> signalGroupId) {
    		int onset;
		int dropping;
		int signalSystemOffset;
		if (signalGroupsFirstODPair.contains(signalGroupId)) {
			if (signalGroupsAllRight.contains(signalGroupId)) {
				onset = 0;
				dropping = 15 - INTERGREEN_TIME;
				signalSystemOffset = 0;
			} else {
				onset = 15;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
			}
		} else {
			if (signalGroupsAllRight.contains(signalGroupId)) {
				onset = 30;
				dropping = 45 - INTERGREEN_TIME;
				signalSystemOffset = 0;
			} else {
				onset = 45;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
			}
		}
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, signalGroupId, onset, dropping));
		signalPlan.setOffset(signalSystemOffset);
	}

	public void writeSignalFiles(String directory) {
		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);

        new SignalSystemsWriter20(signalsData.getSignalSystemsData()).write(directory + "signalSystems.xml");
        new SignalControlWriter20(signalsData.getSignalControlData()).write(directory + "signalControl.xml");
        new SignalGroupsWriter20(signalsData.getSignalGroupsData()).write(directory + "signalGroups.xml");
    }

}

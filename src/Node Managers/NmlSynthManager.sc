NmlSynthManager : NmlAbstract {
	var < synthDef;
	var < nodes;
	var < nodeInfo;
	var < group;

	var < processDataTrigger;
	var < processDataRetrigger;
	var < processDataRelease;
	var < processDataSet;

	// var < triggerData;
	// var < retriggerData;
	// var < releaseData;

	var < actionPreTrigger;
	var < actionPreRetrigger;
	var < actionPreRelease;
	var < actionPreSet;
	var < actionPreRun;
	var < actionPostTrigger;
	var < actionPostRetrigger;
	var < actionPostRelease;
	var < actionPostSet;
	var < actionPostRun;
	var < actionOnFree;

	var <> poly = inf;
	var < hold = false;

	var in = 0;
	var out = 0;

	var overwriteMethod = \overwriteRetrigger;

	*new {
		arg target, synthDef;
		^ super.newCopyArgs(synthDef).target_(target).init(synthDef);
	}

	synthDef_ {
		arg a_synthDef;
		synthDef = a_synthDef;
		^ this;
	}

	init {
		arg a_synthDef;
		this.synthDef = a_synthDef;
		super.init();
		group = Group(target);

		nodes = DzDmIndex[];
		nodeInfo = IdentityDictionary[];

		processDataTrigger = List[];
		processDataRetrigger = List[];
		processDataRelease = List[];
		processDataSet = List[];
		processDataTrigger.add(DzDmProcessDefaultValue(this.triggerData));
		processDataRetrigger.add(DzDmProcessDefaultValue(this.retriggerData));
		processDataRelease.add(DzDmProcessDefaultValue(this.releaseData));

		actionPreTrigger = List[];
		actionPreRetrigger = List[];
		actionPreRelease = List[];
		actionPreSet = List[];
		actionPostTrigger = List[];
		actionPostRetrigger = List[];
		actionPostRelease = List[];
		actionPostSet = List[];
		actionOnFree = List[];

		^ this;
	}

	trigger {
		arg data;
		var id, node, result;
		id = this.generateId(data);
		node = nodes.at(id);

		// Retrigger.
		if (this.canActOnNode(node)) {
			if (this.perform(overwriteMethod, id, node, data)) {
				^ node;
			};
		};

		result = this.prTrigger(id, node, data);
		^ result;
	}

	prTrigger {
		arg id, node, data;
		var def;

		data = this.processData(data, processDataTrigger);
		this.runActions(id, node, data, actionPreTrigger);

		def = data.at(\synthDef) ?? { synthDef ?? { \default } };
		node = Synth(def, data.asPairs, group);
		nodes.put(id, node, data[\weight] ? 0);
		NodeWatcher.register(node);
		node.onFree({
			this.runActions(id, node, data, actionOnFree);
			this.prKill(id, node);
		});
		this.addTriggerInfo(id, data, def);
		this.runActions(id, node, data, actionPostTrigger);
		^ node;
	}

	addTriggerInfo {
		arg id, data, def;
		var t = TempoClock.default;
		data = DzDmProcessDefaultValue(IdentityDictionary[
			\beats -> t.beats,
			\beatPos -> (t.beats % 1),
			\barPos -> t.beatInBar,
			\instrument -> def,
			\triggerVel -> data[\vel] ? 0,
		]).copyApply(data);
		this.updateNodeInfo(id, data);
		^ this;
	}

	retrigger {
		arg data;
		var id, node, result;
		if (hold) {
			^ nil
		};

		id = this.generateId(data);
		node = nodes.at(id);

		result = this.prRetrigger(id, node, data);
		^ result;
	}

	prRetrigger {
		arg id, node, data;
		if (this.canActOnNode(node)) {
			data = this.processData(data, processDataRetrigger);
			this.runActions(id, node, data, actionPreRetrigger);
			this.prSet(node, data);
			this.addRetriggerInfo(id, data);
			this.runActions(id, node, data, actionPostRetrigger);
		};

		^ node;
	}

	addRetriggerInfo {
		arg id, data;
		var t = TempoClock.default;
		data = DzDmProcessDefaultValue(IdentityDictionary[
			\beats -> t.beats,
			\beatPos -> (t.beats % 1),
			\barPos -> t.beatInBar,
			\retriggerVel -> data[\vel] ? 0,
		]).copyApply(data);
		this.updateNodeInfo(id, data);
		^ this;
	}

	release {
		arg data;
		var id, node, result, shouldDefer;
		if (hold) {
			^ nil
		};
		id = this.generateId(data);
		node = nodes.at(id);
		shouldDefer = this.canActOnNode.not;
		result = this.prRelease(id, node, data);
		^ result;
	}

	prRelease {
		arg id, node, data;
		if (this.canActOnNode(node)) {
			data = this.processData(data, processDataRelease);
			this.runActions(id, node, data, actionPreRelease);
			this.prSet(node, data);
			this.addReleaseInfo(id, data);
			this.runActions(id, node, data, actionPostRelease);
		};
		^ node;
	}

	addReleaseInfo {
		arg id, data;
		var t = TempoClock.default;
		var oldData = nodeInfo.at(id) ?? { IdentityDictionary[] };
		data = DzDmProcessDefaultValue(IdentityDictionary[
			\dur -> (t.beats - (oldData.at(\beats) ? 0)),
			\release -> t.beats,
			\releaseVel -> (data[\vel] ? 0),
		]).copyApply(data);
		this.updateNodeInfo(id, data);
		^ this;
	}

	set {
		arg data;
		var id, node, result;
		if (hold) {
			^ nil
		};
		id = this.generateId(data);
		node = nodes.at(id);
		if (this.canActOnNode(node)) {
			data = this.processData(data, processDataSet);
			this.runActions(id, node, data, actionPreSet);
			this.prSet(node, data);
			this.runActions(id, node, data, actionPostSet);
		};
		^ node;
	}

	prSet {
		arg node, data;
		node.set(*(data.asPairs));
		^ this;
	}

	run {
		arg id, run = true;
		var node;
		node = nodes.at(id);
		if (this.canActOnNode(node)) {
			var data = Event[\run -> run];
			this.runActions(id, node, data, actionPreRun);
			node.run(data[\run]);
			this.runActions(id, node, data, actionPostRun);
		};
		^ node;
	}

	pause {
		arg id;
		^ this.run(id, false);
	}

	kill {
		arg data;
		var id, node, result;
		id = this.generateId(data);
		node = nodes.at(id);
		result = this.prKill(id, node);
		^ result;
	}

	prKill {
		arg id, node;
		node.free;
		nodes.removeAt(id);
		nodeInfo.removeAt(id);
		^ true;
	}

	setAll {
		arg data;
		data = this.processData(data, processDataSet);
		nodes.keysValuesDo {
			arg id, node;
			this.runActions(id, node, data, actionPreSet);
			this.prSet(node, data);
			this.runActions(id, node, data, actionPostSet);
		};
		^ this;
	}

	processData {
		arg data, processes;
		data = data.copy;
		processes.do {
			arg process;
			process.apply(data);
		};
		^ data;
	}

	updateNodeInfo {
		arg id, data;
		var oldData = nodeInfo.at(id) ?? { IdentityDictionary[] };
		oldData = DzDmProcessDefaultValue(oldData).copyApply(data);
		nodeInfo.put(id, oldData);
		^ oldData;
	}

	runActions {
		arg id, node, data, actions;
		actions.do {
			arg action;
			action.value(id, node, data);
		};
		^ this;
	}

	canActOnNode {
		arg node;
		this.sync();
		^ node.notNil and: { node.isPlaying };
	}

	/**
	 * Retrigger the previous node, and confirm node is still playing.
	 */
	overwriteRetrigger {
		arg id, node, data;
		this.prRetrigger(id, node, data);
		^ node.isPlaying();
	}

	/**
	 * Release the previous node. Execution should always continue at this point.
	 */
	overwriteRelease {
		arg id, node, data;
		this.prRelease(id, node, data);
		^ false;
	}

	makeSpaceForPoly {
		arg id, data;

		// selectors.do {
		// 	arg selector;
		// 	var matches = selector.apply(nodes);
		// 	if (matches.size > poly) {
		// 		// @TODO
		// 	};
		// }

	}

	/**
	 * Run an audit of actual nodes on the server.
	 */
	auditNodes {
		arg func ... args;
		var s = target.server;
		OSCFunc({
			arg msg;
			func.(msg, *args);
		}, '/g_queryTree.reply', s.addr).oneShot;
		s.sendMsg("/g_queryTree", group.nodeID);
		^ this;
	}

	/**
	 * Perform a function on all nodes on the server.
	 */
	prGetNodesFromTreeResponse {
		arg func, class = Node ... args;
		var auditFunc = {
			arg msg, class = Node;
			var nodeList = List[];
			var s = target.server;
			msg = msg.reverse;
			msg.pop;
			msg.pop;
			while({msg.size > 0}) {
				var nodeId = msg.pop;
				var type = msg.pop;
				if (type == -1) {
					var synthDef = msg.pop;
					nodeList.add(Synth.basicNew(synthDef, s, nodeId));
				} {
					nodeList.add(Group.basicNew(s, nodeId));
				};
			};
			nodeList = nodeList.select {
				arg node;
				node.isKindOf(class);
			};
			func.(nodeList, *args);
		};
		^ this.auditNodes(auditFunc, class);
	}

	prGetSynthsFromTreeResponse {
		arg func ... args;
		^ this.prGetNodesFromTreeResponse(func, Synth, *args);
	}

	/**
	 * Occasionally, a node will get lost from the dictionary. This is usually
	 * due to race conditions. This method will scan the server and release as
	 * needed.
	 */
	releaseOrphanedNodes {
		this.prGetSynthsFromTreeResponse({
			arg a_nodes;
			a_nodes.do {
				arg node;
				[\node, node, nodes.values].postln;
				if (nodes.values.includes(node).not) {
					node.set(*(this.releaseData.asPairs));
				};
			};
		});
		^ this;
	}

	setInOut {
		arg a_in, a_out;
		in = a_in;
		out = a_out;
		^ this;
	}

	triggerData {
		^ IdentityDictionary[
			\t_sync -> 1,
			\gate -> 1,
			\in -> in,
			\out -> out,
		];
	}

	retriggerData {
		^ IdentityDictionary[
			\t_sync -> 1,
			\gate -> 1,
			\in -> in,
			\out -> out,
		];
	}

	releaseData {
		^ IdentityDictionary[
			\t_sync -> 0,
			\gate -> 0,
			\in -> in,
			\out -> out,
		];
	}

	/**
	 * see @releaseOrphanedNodes
	 * Runs an audit and releases nodes every second.
	 */
	runNodeAuditRelease {
		arg interval = 1;
		^ Routine {
			inf.do {
				this.releaseOrphanedNodes();
				interval.wait;
			};
		}.play;
	}

	asTarget {
		^ group;
	}

}

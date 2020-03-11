NmlSynthManager : NmlAbstract {
	var < synthDef;
	var < nodes;
	var < nodeInfo;

	var < processDataTrigger;
	var < processDataRetrigger;
	var < processDataRelease;
	var < processDataSet;

	var < actionPreTrigger;
	var < actionPreRetrigger;
	var < actionPreRelease;
	var < actionPreSet;
	var < actionPostTrigger;
	var < actionPostRetrigger;
	var < actionPostRelease;
	var < actionPostSet;
	var < actionOnFree;

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

		nodes = IdentityDictionary[];
		nodeInfo = IdentityDictionary[];

		processDataTrigger = List[];
		processDataRetrigger = List[];
		processDataRelease = List[];
		processDataSet = List[];
		processDataTrigger.add(DzDmProcessDefaultValue(IdentityDictionary[
			\t_sync -> 1,
			\gate -> 1,
		]));
		processDataRetrigger.add(DzDmProcessDefaultValue(IdentityDictionary[
			\t_sync -> 1,
			\gate -> 1,
		]));
		processDataRelease.add(DzDmProcessDefaultValue(IdentityDictionary[
			\t_sync -> 0,
			\gate -> 0,
		]));

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
		var id = this.generateId(data);
		var node = nodes.at(id);

		// Retrigger.
		if (this.canActOnNode(node)) {
			if (this.perform(overwriteMethod, id, node, data)) {
				^ node;
			};
		};

		^ this.prTrigger(id, node, data);
	}

	prTrigger {
		arg id, node, data;
		var def;

		data = this.processData(data, processDataTrigger);
		this.runActions(id, node, data, actionPreTrigger);

		def = data.at(\synthDef) ?? { synthDef ?? { \default } };
		node = Synth(def, data.asPairs);
		nodes.put(id, node);
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
		var id = this.generateId(data);
		var node = nodes.at(id);

		^ this.prRetrigger(id, node, data);
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
		var id = this.generateId(data);
		var node = nodes.at(id);

		^ this.prRelease(id, node, data);
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
		var id = this.generateId(data);
		var node = nodes.at(id);
		if (this.canActOnNode(node)) {
			data = this.processData(data, processDataSet);
			this.runActions(id, node, data, actionPreSet);
			this.prSet(node, data);
			this.runActions(id, node, data, actionPostSet);
		};
		^ node;
	}

	kill {
		arg data;
		var id = this.generateId(data);
		var node = nodes.at(id);

		^ this.prKill(id, node);
	}

	prKill {
		arg id, node;
		node.free;
		nodes.removeAt(id);
		nodeInfo.removeAt(id);
	}

	prSet {
		arg node, data;
		node.set(*(data.asPairs));
		^ this;
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

}

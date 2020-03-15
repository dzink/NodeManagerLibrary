NmlEffects : NmlSynthManager {
	var processDataOn;
	var processDataOff;
	var actionPreOn;
	var actionPreOff;
	var actionPostOn;
	var actionPostOff;

	init {
		arg ... args;
		super.init(*args);

		processDataOn = List[];
		processDataOff = List[];
		processDataOn.add(DzDmProcessDefaultValue(this.onData));
		processDataOff.add(DzDmProcessDefaultValue(this.offData));

		actionPreOn = List[];
		actionPreOff = List[];
		actionPostOn = List[];
		actionPostOff = List[];

		^ this;
	}

	add {
		arg synthDef, data, weight;
		var id, node;
		synthDef = synthDef ?? { data.at(\synthDef) ?? data.at(\instrument) };
		weight = weight ?? { data.at(\weight) ? 0 };
		data[\synthDef] = synthDef;
		data[\weight] = weight;
		data[\id] = data[\id] ? synthDef;
		node = this.trigger(data);
		this.sort();
		^ node;
	}

	effectOn {
		arg id, data;
		var node, result, shouldDefer;
		node = nodes.at(id);
		result = this.prEffectOn(id, node, data);
		^ result;
	}

	prEffectOn {
		arg id, node, data;
		if (this.canActOnNode(node)) {
			data = this.processData(data, processDataOn);
			this.runActions(id, node, data, actionPreOn);
			this.prSet(node, data);
			this.runActions(id, node, data, actionPostOn);
		};
		^ node;
	}

	effectOff {
		arg id, data;
		var node, result, shouldDefer;
		node = nodes.at(id);
		result = this.prEffectOff(id, node, data);
		^ result;
	}

	prEffectOff {
		arg id, node, data;
		if (this.canActOnNode(node)) {
			data = this.processData(data, processDataOff);
			this.runActions(id, node, data, actionPreOff);
			this.prSet(node, data);
			this.runActions(id, node, data, actionPostOff);
		};
		^ node;
	}

	onData {
		^ IdentityDictionary[
			\mix -> 1,
		];
	}

	offData {
		^ IdentityDictionary[
			\mix -> 0,
		];
	}

	generateId {
		arg data;
		^ data[\id] ?? { data[\synthDef] };
	}

	sort {
		// @TODO find a more efficient way to do this.
		// Maybe query nodes, and see if they're in the right order?
		var previous = nil;
		nodes.values.do {
			arg node;
			if (previous.isNil) {
				node.moveToHead(group);
			} {
				node.moveAfter(previous);
			};
			previous = node;
		};
		^ this;
	}
}

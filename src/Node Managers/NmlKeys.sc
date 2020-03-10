NmlKeys : NmlSynthManager {
	var < scale;

	*new {
		arg target, synthDef, scale;
		^ super.new(target, synthDef).init(synthDef, scale);
	}

	init {
		arg a_synthDef, a_scale;
		super.init(a_synthDef);
		this.scale = a_scale ?? { Scale.major };
		^ this;
	}

	scale_ {
		arg a_scale;
		if (a_scale.isKindOf(Scale)) {
			a_scale = scale;
			^ this;
		};
		Exception("Tried to add a non-scale to an NmlKeys").throw();
	}

	generateId {
		arg data;
		^ data.at(\id) ?? { data.at(\note) ?? { super.generateId(data) }};
	}

	midiConnect {
		arg noteNum = nil, channels = nil;

	}

	midiConnectNoteOn {
		arg noteNum = nil, channels = nil;
		^ MIDIFunc.noteOn({
			arg vel, note ... args;
			this.trigger(Event[
				\note -> note,
				\vel -> vel,
			]);
		}, noteNum, channels);
	}

	midiConnectNoteOff {
		arg noteNum = nil, channels = nil;
		^ MIDIFunc.noteOff({
			arg vel, note ... args;
			this.release(Event[
				\note -> note,
				\vel -> vel,
			]);
		}, noteNum, channels);
	}

	midiConnectPolytouch {
		arg noteNum = nil, channels = nil;
		^ MIDIFunc.polytouch({
			arg vel, note ... args;
			this.set(Event[
				\note -> note,
				\vel -> vel,
			]);
		}, noteNum, channels);
	}

	addFreqToData {
		arg data;
		data[\freq] = data[\freq] ?? { Scale.major.degreeToFreq(data.at(\note), 440, -7) };
		^ data;
	}

}

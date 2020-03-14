TestNmlSynthManager : TestNml {
	var m;

	setUp {
		m = NmlSynthManager();
	}

	tearDown {
		m.free
	}

	test_poly {
		m.poly = 3;
		// m.trigger(Event[\note -> 1]);
		// m.trigger(Event[\note -> 2]);
		// m.trigger(Event[\note -> 3]);
		// m.trigger(Event[\note -> 4]);

		m.nodes.size.postln;
	}
}

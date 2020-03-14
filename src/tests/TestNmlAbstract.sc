TestNmlAbstract : TestNml {
	var n;

	setUp {
		n = NmlAbstract();
	}

	tearDown {
		n.free;
	}

	test_target {
		this.assert(n.target_(Server.default.asTarget).target.isKindOf(Server), "Servers can be added as target");
		this.assert(n.target_(Group()).target.isKindOf(Node), "Nodes can be added as target");
	}

	test_id {
		var data = IdentityDictionary[\id -> \testId, \a -> 1];
		var test = IdentityDictionary[\a -> 1];

		this.assertEquals(n.generateId(data), \testId, "Generate Id uses \\id key if possible.");
		data.removeAt(\id);
		this.assert(n.generateId(data).isKindOf(Integer), "Generate Id uses a hash otherwise.");
		this.assertEquals(n.generateId(data), n.generateId(test), "Generate Id creates the same hash for similar objects.");
	}
}

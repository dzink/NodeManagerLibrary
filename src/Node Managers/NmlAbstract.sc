NmlAbstract {
	var < target;
	var < semaphore;

	*new {
		arg target;
		^ super.newCopyArgs(target).init();
	}

	init {
		semaphore = Semaphore(1);
	}

	target_ {
		arg a_target;
		a_target = a_target ?? { Server.default };
		[Node, Server].do {
			arg class;
			if (a_target.isKindOf(class)) {
				target = a_target;
				^ this;
			};
		};
		Exception("Tried to add a bad target to a DzNmAbstract").throw();
	}

	/**
	 * Based on data, generate an id that will be used to identify this node.
	 */
	generateId {
		arg data;
		^ data.at(\id) ?? { data.hash };
	}

	sync {
		if (thisThread.isKindOf(Routine)) {
			if (target.isKindOf(Server)) {
				target.sync();
			} {
				target.server.sync();
			};
		};
	}

}

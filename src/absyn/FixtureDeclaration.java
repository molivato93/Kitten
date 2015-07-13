package absyn;

import java.io.FileWriter;

import semantical.TypeChecker;
import types.ClassType;
import types.FixtureSignature;
import types.VoidType;

public class FixtureDeclaration extends CodeDeclaration {

	private final Command body;

	public FixtureDeclaration(int pos, Command body, ClassMemberDeclaration next) {
		super(pos, null, body, next);
		this.body = body;
	}

	@Override
	protected void toDotAux(FileWriter where) throws java.io.IOException {
		linkToNode("body", body.toDot(where), where);
	}
	
	/**
	 * Yields the signature of this fixture declaration.
	 *
	 * @return the signature of this fixture declaration. Yields {@code null}
	 *         if type-checking has not been performed yet
	 */

	@Override
	public FixtureSignature getSignature() {
		return (FixtureSignature) super.getSignature();
	}
	
	@Override
	protected void addTo(ClassType clazz) {
		FixtureSignature mSig = new FixtureSignature(clazz, this);

		clazz.addFixture(mSig);

		// we record the signature of this fixture inside this abstract syntax
		setSignature(mSig);

	}

	@Override
	protected void typeCheckAux(ClassType clazz) {
		TypeChecker checker;

		// Creo un nuovo typechecker
		checker = new TypeChecker(VoidType.INSTANCE, clazz.getErrorMsg());

		// Verifico di non essere dentro un' altra fixture
		if (!checker.isInCodeDeclarationList(this.getClass().getName())) {

			checker.addToCodeDeclarationList(this);

			body.typeCheck(checker);

			checker.removeFromCodeDeclarationList(this);

			if (getBody().checkForDeadcode())
				error(checker, "Deadcode in this Fixture Declaration!");
		} else
			// Errore sono dentro un'altra fixture
			error(checker, "A fixture shouldn't be into another fixture !!!");
	}

}

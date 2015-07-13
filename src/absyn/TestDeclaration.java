package absyn;

import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

import semantical.TypeChecker;
import translation.Block;
import types.ClassMemberSignature;
import types.ClassType;
import types.IntType;
import types.TestSignature;
import types.TypeList;
import types.VoidType;
import bytecode.CONST;
import bytecode.NEWSTRING;
import bytecode.RETURN;
import bytecode.VIRTUALCALL;

public class TestDeclaration extends CodeDeclaration {

	private final String name;
	private final Command body;

	public TestDeclaration(int pos, String name, Command body,
			ClassMemberDeclaration next) {
		// Non ho parametri formali
		super(pos, null, body, next);
		this.name = name;
		this.body = body;
	}

	@Override
	protected void toDotAux(FileWriter where) throws java.io.IOException {
		linkToNode("name", toDot(name, where), where);
		linkToNode("body", body.toDot(where), where);
	}

	public boolean checkForDeadcode() {
		return body.checkForDeadcode();
	}

	public String getName() {
		return name;
	}
	
	/**
	 * Yields the signature of this test declaration.
	 *
	 * @return the signature of this test declaration. Yields {@code null}
	 *         if type-checking has not been performed yet
	 */

	@Override
	public TestSignature getSignature() {
		return (TestSignature) super.getSignature();
	}
	
	@Override
	protected void addTo(ClassType clazz) {
		TestSignature mSig = new TestSignature(clazz, name, this);

		clazz.addTest(name, mSig);

		// we record the signature of this Test inside this abstract syntax
		setSignature(mSig);
	}

	@Override
	protected void typeCheckAux(ClassType clazz) {
		
		// Creo un nuovo typechecker
		TypeChecker checker = new TypeChecker(VoidType.INSTANCE, clazz.getErrorMsg());

		if (!checker.isInCodeDeclarationList(this.getClass().getName())) {
			checker.addToCodeDeclarationList(this);

			getBody().typeCheck(checker);

			checker.removeFromCodeDeclarationList(this);

			if (getBody().checkForDeadcode())
				error(checker, "Deadcode in this Test Declaration!");
		} else
			error(checker, "A Test shouldn't be into another Test !!!");
	}
	
	/**
	 * Translates this constructor or method into intermediate Kitten code. This
	 * amounts to translating its body with a continuation containing a
	 * {@code return} bytecode. This way, if a method does not have an explicit
	 * {@code return} statement, it is automatically put at its end.
	 *
	 * @param done
	 *            the set of code signatures that have been already translated
	 */
	@Override
	public void translate(Set<ClassMemberSignature> done) {
		if (done.add(this.getSignature())) {
			
			//Creo il messaggio di errore che voglio stampare
			String passedStr = "passed ";
			
			//Creo il tipo della classe che voglio chiamare
			ClassType cts = ClassType.mkFromFileName("String.kit");
			
			//Creo il blocco me mi stampa che il test è passato con successo e ritorna 0
			Block testPassed = new Block(new RETURN(IntType.INSTANCE));
			testPassed = new CONST(0).followedBy(testPassed);
			testPassed = new NEWSTRING(passedStr)
					.followedBy(new VIRTUALCALL(cts, cts.methodLookup("output",
							TypeList.EMPTY)).followedBy(testPassed));

			this.getSignature().setCode(getBody().translate(this.getSignature(),
					testPassed));

			// we translate all methods and constructors that are referenced
			// from the code we have generated
			translateReferenced(this.getSignature().getCode(), done, new HashSet<Block>());
		}
	}

}

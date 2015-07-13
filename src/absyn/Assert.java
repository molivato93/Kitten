package absyn;

import java.io.FileWriter;

import semantical.TypeChecker;
import translation.Block;
import types.ClassType;
import types.CodeSignature;
import types.IntType;
import types.TypeList;
import bytecode.CONST;
import bytecode.NEWSTRING;
import bytecode.RETURN;
import bytecode.VIRTUALCALL;
import errorMsg.ErrorMsg;

public class Assert extends Command{
	
	private final Expression condition;
	
	public Assert(int pos, Expression condition) {
		super(pos);
		this.condition = condition;
	}
	
	public Expression getCondition() {
		return condition;
	}

	@Override
	protected void toDotAux(FileWriter where) throws java.io.IOException {
		linkToNode("condition", condition.toDot(where), where);
	}
	
	@Override
	protected TypeChecker typeCheckAux(TypeChecker checker) {
		
		//Verifico che mi abbia controllato un comando Test
		if(checker.isInCodeDeclarationList(TestDeclaration.class.getName()))
			condition.mustBeBoolean(checker);
		else
			//Errore non sono in un comando test
			error("Assert must be into a Test declaration !");
		
		return checker;
	}

	@Override
	public boolean checkForDeadcode() {
		//Non ho comandi da eseguire perciò non posso avere deadcode
		return false;
	}

	/*
	@Override
	public Block translate(CodeSignature where, Block continuation) {
		// by making the continuation unmergeable with whatever we
		// prefix to it, we avoid duplicating it in the then and
		// else branch. This is just an optimisation!
		// Try removing this line: everything will work, but the code will be larger		
		continuation.doNotMerge();
		
		//Creo il messaggio di errore che voglio stampare
		String errorStr = "test fallito @"
						  + where.getDefiningClass()
						  + ".kit:" + ErrorMsg.getWhereInFile(this.getPos());
		
		//Creo il tipo della classe che voglio chiamare
		ClassType cts = ClassType.mk("String");
		
		//Creo l'effettivo blocco tradotto che mi stampa il messaggio di errore
		Block assertFailure = new NEWSTRING(errorStr)
				.followedBy(new VIRTUALCALL(cts, cts.methodLookup("output",
						TypeList.EMPTY)).followedBy(continuation));
		
		//Creo il blocco che ritorna la stringa vuota segno di verifica dell'assert
		Block assertVerified = new NEWSTRING("")
				.followedBy(new VIRTUALCALL(cts, cts.methodLookup("output",
						TypeList.EMPTY)).followedBy(continuation));
		
		//Ritorno il blocco verificando le condizioni e traducendo i blocchi 
		return condition.translateAsTest(where, assertVerified, assertFailure);
	}*/
	
	@Override
	public Block translate(CodeSignature where, Block continuation) {
		// This is just an optimisation!
		continuation.doNotMerge();
		
		//Creo il messaggio di errore che voglio stampare
		/*String errorStr = "assert fallito @"
						  + where.getDefiningClass()
						  + ".kit:" + ErrorMsg.getWhereInFile(this.getPos());*/
		String errorStr = "failed at "+ ErrorMsg.getWhereInFile(this.getPos());
		
		//Creo il tipo della classe che voglio chiamare
		ClassType cts = ClassType.mkFromFileName("String.kit");
		
		//Creo l'effettivo blocco tradotto che mi stampa il messaggio di errore e ritorna 1
		Block assertFailure = new Block(new RETURN(IntType.INSTANCE));
		assertFailure = new CONST(1).followedBy(assertFailure);
		assertFailure = new NEWSTRING(errorStr)
				.followedBy(new VIRTUALCALL(cts,
						cts.methodLookup("output", TypeList.EMPTY)).followedBy(assertFailure));
		
		
		//Ritorno il blocco verificando le condizioni e traducendo i blocchi 
		return condition.translateAsTest(where, continuation, assertFailure);
	}

}

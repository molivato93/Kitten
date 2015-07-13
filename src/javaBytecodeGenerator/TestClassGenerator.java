package javaBytecodeGenerator;

import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import types.ClassMemberSignature;
import types.ClassType;
import types.FixtureSignature;
import types.TestSignature;

@SuppressWarnings("serial")
public class TestClassGenerator extends JavaClassGenerator {
	
	public TestClassGenerator(ClassType clazz, Set<ClassMemberSignature> sigs) {
		
		super(clazz.getName()+"Test", // name of the class
			// the superclass of the Kitten Object class is set to be the Java java.lang.Object class
			clazz.getSuperclass() != null ? clazz.getSuperclass().getName() : "java.lang.Object",
			clazz.getName() + ".kit" // source file name
			); 
		
		//Aggiungo le fixtures
		for(FixtureSignature fixture: clazz.getFixtures())
			if(sigs == null || sigs.contains(fixture))
				fixture.createFixture(this);
		
		//Aggiungo i Test
		for(TestSignature test: clazz.getTests().values())
			if(sigs == null || sigs.contains(test))
				test.createTest(this);
		
		//Aggiungo il main
		createMainForTests(clazz);		
	}
	
	/**
	 * Creo il metodo main per la classe di test che esegue tutti i test.
	 * e per ogni test tutte le fixture, sull'oggetto della classe da testare.
	 * @param clazz {@code ClassType} della classe da testare.
	 */
	private void createMainForTests(ClassType clazz){
		MethodGen methodGen;
		methodGen = new MethodGen
				(Constants.ACC_PUBLIC | Constants.ACC_STATIC, // public and static
				org.apache.bcel.generic.Type.VOID, // return type
				new org.apache.bcel.generic.Type[] // parameters
					{ new org.apache.bcel.generic.ArrayType("java.lang.String", 1) },
				null, // parameters names: we do not care
				"main", // method's name
				getClassName(), // defining class
				createInstructionListMainForTests(clazz), // bytecode of the method
				getConstantPool()); // constant pool
		
		// we must always call these methods before the getMethod()
		// method below. They set the number of local variables and stack
		// elements used by the code of the method
		methodGen.setMaxStack();
		methodGen.setMaxLocals();

		// we add a method to the class that we are generating
		addMethod(methodGen.getMethod());
	}
	
	/**
	 * Creo la lista di istruzioni che compone il main per la classe di test.
	 * @param clazz {@code ClassType} che contiene tutti i tipi e le signature della classe da testare.
	 * @return {@code InstructionList} che esegue il main.
	 */
	private InstructionList createInstructionListMainForTests(ClassType clazz){
		
		//Creo la lista di istruzioni da eseguire
		InstructionList instructions = new InstructionList();
		
		//Stampo l'inizo dell'esecuzione dei Test per questa classe
		instructions.append(printThisStr("Test execution for class "+clazz.getName()+"\n"));
		
		//Aggiungo una variabile contatore per i test falliti 
		instructions.append(setupCounter());
		
		//Eseguo tutti i test della classe
		for (TestSignature test: clazz.getTests().values())
			//Aggiungo il test e aggiungo inoltre il controllo
			//sul tempo di esecuzione e la relativa stampa.
			instructions.append(deltaTimeMillis(testILGenerator(test,  clazz)));
		
		
		//Creo il codice per contare e stampare i test passati
		InstructionList testPassed = new InstructionList();
		//Carico il numero totale di test totali della classe
		testPassed.append(new LDC(getConstantPool().addInteger(clazz.getTests().size())));
		//Sottraggo i test falliti ai test totali
		testPassed.append(InstructionFactory.ILOAD_1);
		testPassed.append(InstructionFactory.ISUB);
		
		//Stampo il numero di test passati e la stringa
		instructions.append(printThisIntIL(testPassed));
		instructions.append(printThisStr(" test(s) passed, "));
		
		//Codice per stampare i test falliti
		InstructionList testFailed = new InstructionList();
		//Recupero il numero di test falliti
		testFailed.append(InstructionFactory.ILOAD_1);
		//Stampo il numero di test falliti e la stringa
		instructions.append(printThisIntIL(testFailed));
		instructions.append(printThisStr(" failed "));
		
		//Controllo il tempo di esecuzione di tutti i test
		instructions = deltaTimeMillis(instructions);
		
		//Ritorno Void
		instructions.append(InstructionFactory.createReturn(Type.VOID));
		return instructions;
	}
	
	/**
	 * Eseguo il setup di una variabile contatore (al valore 0)
	 * all'interno della variabile di indice 1.
	 * @return {@code InstructionList} che esegue il setup del contatore.
	 */
	private InstructionList setupCounter(){
		InstructionList il = new InstructionList();
		
		//Aggiungo una variabile contatore per i test falliti 
		il.append(InstructionFactory.ICONST_0);
		il.append(InstructionFactory.ISTORE_1);
		
		return il;
	}
	
	/**
	 * Genero il Java Bytecode che esegue un test.
	 * @param test signature del test da eseguire.
	 * @param clazz {@code ClassType} della calsse su cui devo eseguire i test.
	 * @return l'instruction list che esegue il test.
	 */
	private InstructionList testILGenerator(TestSignature test, ClassType clazz){
		InstructionList testIL = new InstructionList();
		
		//Stampo il test che sto eseguendo
		testIL.append(printThisStr("\n\t- " + test.getName() + ": "));
		
		//Creo un nuovo oggetto della classe
		testIL.append(createNewObject(clazz.getName()));
		
		//Eseguo tutte le fixtures di questa classe
		for (FixtureSignature fixture : clazz.getFixtures())
			testIL.append(invokeStaticMethod(clazz.getName()+"Test", fixture.getName(), clazz.toBCEL(), true, true));
		
		//Eseguo il test
		testIL.append(this.invokeStaticMethod(test.getDefiningClass().getName()+"Test", test.getName(), clazz.toBCEL(), false, false));
		
		//Salvo il numero di test falliti incrementando la variabile contatore
		testIL.append(InstructionFactory.ILOAD_1);
		testIL.append(InstructionFactory.IADD);
		testIL.append(InstructionFactory.ISTORE_1);
		
		//Ritorno l'instruction list del test appena creata
		return testIL;
	}
	
	/**
	 * Creo un nuovo oggetto della classe passata come parametro.
	 * @param objname nome della classe di cui si vuole l'oggetto.
	 * @return l'instruction list della creazione dell'oggetto.
	 */
	private InstructionList createNewObject(String objname){
		InstructionList il = new InstructionList();
		
		//Creo la new dell'oggetto
		il.append(getFactory().createNew(objname));
		//Duplico il ricevitore
		il.append(InstructionFactory.DUP);
		//Chiamo effettivamente il costruttore
		il.append(getFactory().createInvoke(objname, "<init>", 
				org.apache.bcel.generic.Type.VOID, 
				new org.apache.bcel.generic.Type[]{},
				org.apache.bcel.Constants.INVOKESPECIAL ));
		
		return il;
	}
	
	/**
	 * Invoco un metodo statico su un oggetto.
	 * @param classname nome della calssse su cui deve essere chiamata la funzione.
	 * @param methodname nome del metodo da invocare.
	 * @param argtype tipo dell'argomento da passare alla funzione.
	 * @param dup {@code true} se voglio duplicare l'oggetto su cui chiamo la funzione, altrimenti {@code false}.
	 * @return l'instruction list con la chimata al metodo statico.
	 */
	private InstructionList invokeStaticMethod(String classname, String methodname, Type argtype, boolean dup, boolean isretvoid){
		InstructionList il = new InstructionList();
		
		//Controllo se voglio duplicarlo prima della chiamata o no
		if(dup) il.append(InstructionFactory.DUP);
		
		//Chiamo il metodo statico corretto dell'oggetto
		il.append(getFactory().createInvoke(classname, methodname,
				isretvoid ? org.apache.bcel.generic.Type.VOID : org.apache.bcel.generic.Type.INT,
				argtype!=null ? new org.apache.bcel.generic.Type[] { argtype } : new org.apache.bcel.generic.Type[] {},
				org.apache.bcel.Constants.INVOKESTATIC));
		
		return il;
	}
	
	/**
	 * Stampo una stringa utilizzando la funzione print di System.out .
	 * @param s la stringa da stampare a video.
	 * @return L'instruction list che contiene la stampa a video della stringa s.
	 */
	private InstructionList printThisStr(String s){
		
		//Creo un instruction List per le stampe 
		InstructionList il = new InstructionList();
		il.append(getFactory().createGetStatic("java/lang/System", "out", 
				Type.getType(java.io.PrintStream.class)));
		//Aggiungo la sttringa da stampare
		il.append(new LDC(getConstantPool().addString(s)));
		//Stampo la stringa
		il.append(getFactory().createInvoke("java/io/PrintStream", "print", Type.VOID, 
				new org.apache.bcel.generic.Type[]{org.apache.bcel.generic.Type.STRING},
				org.apache.bcel.Constants.INVOKEVIRTUAL ));
		//Ritorno l'instruction list
		return il;
	}
	
	/**
	 * Stampo un intero usando la funzione print di System.out .
	 * @param ilv l'instruction list che contiene il valore intero da stampare.
	 * @return {@code InstructionList} che stampa l'intero.
	 */
	private InstructionList printThisIntIL(InstructionList ilv){
		InstructionList il = new InstructionList();
		
		//Aggiungo il ricevitore 
		il.append(getFactory().createGetStatic("java/lang/System", "out", 
						Type.getType(java.io.PrintStream.class)));
		//Aggiungo l'intero da stampare
		il.append(ilv);
		//Aggiungo la chiamata alla funzione print
		il.append(getFactory().createInvoke( "java/io/PrintStream", "print", Type.VOID, 
				new org.apache.bcel.generic.Type[]{org.apache.bcel.generic.Type.INT},
				org.apache.bcel.Constants.INVOKEVIRTUAL ));
		
		return il;
	}
	
	/**
	 * Inserisco un controllo dell'esecuzione di un gruppo di istruzioni.
	 * La precisione del conteggio è sui microsecondi.
	 * @param il l'instruction list di cui deve essere misurato il tempo di esecuzione.
	 * @return un instruction list controllato.
	 */
	private InstructionList deltaTimeMillis(InstructionList il){
		
		InstructionList ilDTM = new InstructionList();
		ilDTM.append(getFactory().createGetStatic("java/lang/System", "out", Type.getType(java.io.PrintStream.class)));
		//Recupero il tempo di esecuzione all'inizio della funzione
		ilDTM.append(getNanoTime());
		
		//Aggiungo la funzione da eseguire
		ilDTM.append(il);
		
		//Recupero il tempo di esecuzione all'inizio della funzione
		ilDTM.append(getNanoTime());
		//Sottraggo i due Long sullo stack
		ilDTM.append(InstructionConstants.LSUB);
		//Nego il risultato della sottrazione
		ilDTM.append(InstructionConstants.LNEG);
		//Converto il Long in intero
		ilDTM.append(InstructionConstants.L2I);
		
		//Lo porto ai microsecondi
		ilDTM.append(new LDC(getConstantPool().addInteger(1000)));
		//Lo divido e lo converto in float
		ilDTM.append(InstructionConstants.IDIV);
		ilDTM.append(InstructionConstants.I2F);
		
		//Lo porto ai millisecondi
		ilDTM.append(new LDC(getConstantPool().addFloat(1000)));
		//Divido il float
		ilDTM.append(InstructionConstants.FDIV);
		
		//Stampo il tempo di esecuzione
		ilDTM.append(printThisStr(" ["));
		ilDTM.append(getFactory().createInvoke( "java/io/PrintStream", "print", Type.VOID, 
				new org.apache.bcel.generic.Type[]{org.apache.bcel.generic.Type.FLOAT},
				org.apache.bcel.Constants.INVOKEVIRTUAL ));
		ilDTM.append(printThisStr(" ms]\n"));
		
		return ilDTM;
	}
	
	/**
	 * Recupero il valore temporale in nanosecondi attuale.
	 * @return l'instruction list che recupera il valore attuale in nanosecondi del tempo.
	 */
	private InstructionList getNanoTime(){
		InstructionList il = new InstructionList();
		
		il.append(getFactory().createInvoke( "java/lang/System", "nanoTime", 
				org.apache.bcel.generic.Type.LONG, 
				new org.apache.bcel.generic.Type[]{},
				org.apache.bcel.Constants.INVOKESTATIC ));
		
		return il;
	}
	
}

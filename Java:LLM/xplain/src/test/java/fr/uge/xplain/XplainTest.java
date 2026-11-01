package fr.uge.xplain;

import fr.uge.xplain.compilation.Compiler;
import fr.uge.xplain.controllers.LLM;
import fr.uge.xplain.dto.JavaClassRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class XplainTest {

  @Nested
  public class TestLLM {

    @Test
    public void badIndexToLoad() {
      var llm = new LLM();
      assertThrows(ArrayIndexOutOfBoundsException.class, () -> llm.loadModel(3));
    }

    @Test
    public void llmCreated() throws IOException {
      var llm = new LLM();
      assertDoesNotThrow(() -> llm.loadModel(0));
    }

    @Test
    public void changedLLM() throws IOException {
      var llm = new LLM();
      llm.loadModel(0);
      assertNotNull(llm.abstractModel());
    }

    @Test
    public void changed2timesLLM() throws IOException {
      var llm = new LLM();
      llm.loadModel(0);
      assertNotNull(llm.abstractModel());
      llm.loadModel(1);
      assertNotNull(llm.abstractModel());
    }

    @Test
    void isAlreadyLoaded() throws IOException {
      var llm = new LLM();
      assertEquals(0, llm.models().get("tjake/Llama-3.2-1B-Instruct-JQ4"));
      llm.loadModel(0);
      assertEquals(1, llm.models().get("tjake/Llama-3.2-1B-Instruct-JQ4"));
    }
  }

  @Nested
  public class TestCompiler {

    @Test
    public void emptyClass() {
      Compiler compiler = new Compiler("");
      assertAll(
              () -> assertFalse(compiler.compile()),
              () -> assertEquals("Erreur de syntaxe, veuillez revoir le nom et la déclaration de la classe", compiler.getErrorMessage())
      );
    }

    @Test
    public void badDecalarationClass() {
      Compiler compiler = new Compiler("aaa");
      assertAll(
              () -> assertFalse(compiler.compile()),
              () -> assertEquals("Erreur de syntaxe, veuillez revoir le nom et la déclaration de la classe", compiler.getErrorMessage())
      );
    }

    @Test
    public void annotation() {
      Compiler compiler = new Compiler("public @interface Author {\n" +
              "String name();\n" +
              "String company();\n" +
              "int year() default 0; \n" +
              "}"
      );
      assertAll(
              () -> assertTrue(compiler.compile()),
              () -> assertEquals("", compiler.getErrorMessage())
      );
    }
  }

  @Nested
  public class Explanation {

    @Test
    public void testConstructorAndGetters() {
      var explanation = new fr.uge.xplain.bd.Explanation(
              "TestClass",
              "Syntax Error",
              "Missing semicolon",
              "Add a semicolon"
      );

      assertEquals("TestClass", explanation.getJavaClass());
      assertEquals("Syntax Error", explanation.getError());
      assertEquals("Missing semicolon", explanation.getXplanation());
      assertEquals("Add a semicolon", explanation.getCorrection());
    }

    @Test
    public void testConstructorWithNullValues() {
      assertThrows(NullPointerException.class, () -> {
        new fr.uge.xplain.bd.Explanation(null, "Error", "Explanation", "Correction");
      });

      assertThrows(NullPointerException.class, () -> {
        new fr.uge.xplain.bd.Explanation("Class", null, "Explanation", "Correction");
      });

      assertThrows(NullPointerException.class, () -> {
        new fr.uge.xplain.bd.Explanation("Class", "Error", null, "Correction");
      });

      assertThrows(NullPointerException.class, () -> {
        new fr.uge.xplain.bd.Explanation("Class", "Error", "Explanation", null);
      });
    }

    @Test
    public void testToString() {
      var explanation = new fr.uge.xplain.bd.Explanation(
              "TestClass",
              "Syntax Error",
              "Missing semicolon",
              "Add a semicolon"
      );

      String expected = "TestClass Syntax Error Missing semicolon Add a semicolon";
      assertEquals(expected, explanation.toString());
    }

    @Test
    public void testEquality() {
      var explanation1 = new fr.uge.xplain.bd.Explanation(
              "TestClass",
              "Syntax Error",
              "Missing semicolon",
              "Add a semicolon"
      );

      var explanation2 = new fr.uge.xplain.bd.Explanation(
              "TestClass",
              "Syntax Error",
              "Missing semicolon",
              "new Correction"
      );
      assertNotEquals(explanation1, explanation2);
    }

    @Test
    public void testHashCode() {
      var explanation1 = new fr.uge.xplain.bd.Explanation(
              "TestClass",
              "Syntax Error",
              "Missing semicolon",
              "Add a semicolon"
      );

      var explanation2 = new fr.uge.xplain.bd.Explanation(
              "TestClass",
              "Syntax Error",
              "Missing semicolon",
              "Add a semicolon"
      );

      assertNotEquals(explanation1.hashCode(), explanation2.hashCode());
    }
  }
  @Nested
  public class JavaClassRequestTest {

    @Test
    void testConstructorAndGetters() {
      String classJava = "public class Test {}";
      String errors = "Syntax error on token '}', delete this token";
      String correction = "Add a missing '{' in the class body.";
      String xplanation = "The class body was improperly closed, causing a syntax error.";

      JavaClassRequest request = new JavaClassRequest(classJava, errors, correction, xplanation);
      assertEquals(classJava, request.getClassJava());
      assertEquals(errors, request.getErrors());
      assertEquals(correction, request.getCorrection());
      assertEquals(xplanation, request.getXplanation());
    }

    @Test
    void testConstructorWithNullValues() {
      assertThrows(NullPointerException.class, () -> new JavaClassRequest(null, "errors", "correction", "xplanation"));
      assertThrows(NullPointerException.class, () -> new JavaClassRequest("classJava", null, "correction", "xplanation"));
      assertThrows(NullPointerException.class, () -> new JavaClassRequest("classJava", "errors", null, "xplanation"));
      assertThrows(NullPointerException.class, () -> new JavaClassRequest("classJava", "errors", "correction", null));
    }

    @Test
    void testToString() {
      String classJava = "public class Test {}";
      String errors = "Syntax error on token '}', delete this token";
      String correction = "Add a missing '{' in the class body.";
      String xplanation = "The class body was improperly closed, causing a syntax error.";

      JavaClassRequest request = new JavaClassRequest(classJava, errors, correction, xplanation);
      String toString = request.toString();

      assertTrue(toString.contains(classJava));
      assertTrue(toString.contains(errors));
      assertTrue(toString.contains(correction));
      assertTrue(toString.contains(xplanation));
    }
  }
}
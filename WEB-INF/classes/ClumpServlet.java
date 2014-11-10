import java.math.BigDecimal;

import java.text.DecimalFormat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.lang.ProcessBuilder;

import javax.servlet.MultipartConfigElement;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 * Web Servlet for Clump Automata
 * <p>
 * @author Charles Malaheenee
 * @author Billy Fang
 * @version 0.12 (10.11.2014)
*/

// Charles Malaheenee
@MultipartConfig(maxRequestSize = 15360L, maxFileSize = 10240L)
public class ClumpServlet extends HttpServlet {
   
    // Charles Malaheenee
    private String fProb, fPattern, fTruncate, fA;
    private String fNumberOfStates, fNumberOfEdges, aGraph, aGraphSVG;
    private String fNumberOfStates2, fNumberOfEdges2, aGraph2, aGraphSVG2;
    private String aPoly, fRho, fEval, fFund, fStatus;
    private String fPattFileName, fOutGraphViz;
    private String resFile, resData;
    private static String incJSP, dotPath;
    
    // Billy Fang
    private final int R = 256;
    private final int precision = 15;

    public void init(ServletConfig config)
        throws ServletException {
        super.init(config); 
        
        // Try to find GraphViz executable
        dotPath = this.getServletConfig().getInitParameter("dotPath");
        if (dotPath == null) {
            try {
                ProcessBuilder whichDotPB = new ProcessBuilder("which", "dot");
                whichDotPB.redirectErrorStream(true);
                Process whichDot = whichDotPB.start();
                
                BufferedReader whichDotRead = new BufferedReader(new InputStreamReader(whichDot.getInputStream()));
                dotPath = whichDotRead.readLine();

                whichDotRead.close();
                whichDot.destroy();
            }
            catch (IOException excep) {
                dotPath = null;
            }
        }

        // Try to include jsp form
        incJSP = this.getServletConfig().getInitParameter("includedJSP");
    }

    // Charles Malaheenee
    /**
     * Overrides method doGet for display web-form
    */
    @Override
    protected void doGet(HttpServletRequest request,
                           HttpServletResponse response)
        throws IOException, ServletException {
        // Set session max session time 5 min
        request.getSession().setMaxInactiveInterval(600);
        
        // Set response values
        response.setStatus(200);
        response.setCharacterEncoding("UTF-8");

        // Select Content-Type
        resFile = request.getParameter("resFile");
        resData = request.getParameter("resData");
        
        // Get svg or dot files
        if (resFile != null && resData != null) {
            if (resFile.endsWith("dot")) {
                response.setContentType("text/plain");
            }
            else if (resFile.endsWith("svg")) {
                response.setContentType("image/svg");
                resData = generateGraphViz(resData);
            }
            response.setHeader("Content-Disposition",
                               "attachment;filename=\"" + resFile + "\"");
            OutputStream responseOS = response.getOutputStream();
            getStringAsOutputStream(resData, responseOS);
            responseOS.close();
        }
        // Get html page
        else {
            response.setContentType("text/html");
            // Use included JSP
            if (incJSP != null) {
                RequestDispatcher dispatcher = request.getRequestDispatcher(incJSP);
                dispatcher.forward(request, response);
            }
            else {
                // Print error message
                PrintWriter out = response.getWriter();
                try {
                    out.println("<h2>A problem was occured while loading servlet:</h2>" +
                                "\n<br>\n" +
                                "<label>No definition of JSP form file (\"includedJSP\") in web.xml</label>" +
                                "\n<br>\n" +
                                "\n");
                }
                finally {
                    out.close();
                }
            }
        }
    }

    // Charles Malaheenee
    /**
     * Overrides method doPost for web-form processing
    */
    @Override   
    protected void doPost(HttpServletRequest request,
                            HttpServletResponse response)
        throws ServletException, IOException {
        // Parse parameters
        fProb = request.getParameter("Probabilities");
        fTruncate = request.getParameter("PolyTrunc");
        fA = request.getParameter("A");
        fPattern = request.getParameter("Pattern");
        fOutGraphViz = request.getParameter("OutGraphViz");
        fStatus = null;

        // Load File
        try {
            Part filePart = request.getPart("PattFile");
            if (filePart != null && filePart.getSize() != 0) {
                InputStream filecontent = filePart.getInputStream();
                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(filecontent, "UTF-8"));
                String s;
                while ((s = br.readLine()) != null) {
                    sb.append(s);
                    sb.append("\n");
                }
                fPattern = sb.toString();
            }
        }
        catch (IllegalStateException ex) {
            printError("Error: file too big");
        }
        
        // Start processing
        if (fStatus == null) {
            callClump();
        }

        // Return values
        request.setAttribute("fStatus", fStatus);
        request.setAttribute("fPattern", fPattern);
        request.setAttribute("fProb", fProb);
        request.setAttribute("fA", fA);
        request.setAttribute("fTruncate", fTruncate);
        request.setAttribute("dotPath", dotPath);
        request.setAttribute("clumpPath", request.getRequestURL().toString());

        if (fStatus.startsWith("Successful")) {
            request.setAttribute("fNumberOfEdges", fNumberOfEdges);
            request.setAttribute("fNumberOfStates", fNumberOfStates);
            request.setAttribute("fNumberOfEdges2", fNumberOfEdges2);
            request.setAttribute("fNumberOfStates2", fNumberOfStates2);
            request.setAttribute("aPoly", aPoly);
            request.setAttribute("fRho", fRho);
            request.setAttribute("fEval", fEval);
            request.setAttribute("fFund", fFund);

            if (fOutGraphViz != null) {
                request.setAttribute("fOutGraphViz", fOutGraphViz);
                request.setAttribute("aGraph", aGraph);
                request.setAttribute("aGraph2", aGraph2);
                request.setAttribute("aGraphSVG", generateGraphViz(aGraph));
                request.setAttribute("aGraphSVG2", generateGraphViz(aGraph2));
            }
        }
        doGet(request, response);
    }

    // Charles Malaheenee
    private String generateGraphViz(String dotDigraph)
        throws IOException {
        String dotSVG = ""; 
        if (dotPath == null) {
            dotSVG = null;
        }
        else {
            // Run dot
            ProcessBuilder dotPB = new ProcessBuilder(dotPath, "-Tsvg");
            dotPB.redirectErrorStream(true);
            Process dotProc = dotPB.start();

            // Put data to dot's stdin 
            OutputStream dotProcOS = dotProc.getOutputStream();
            getStringAsOutputStream(dotDigraph, dotProcOS);
            dotProcOS.close();
            
            // Read data from dot's stdout
            BufferedReader dotRead = new BufferedReader(new InputStreamReader(dotProc.getInputStream()));
            String read;
            while ((read = dotRead.readLine()) != null) {
                dotSVG += read;
            }
            dotRead.close();
            dotProc.destroy();
        }
        return dotSVG;
    }
    
    // Charles Malaheenee
    private void getStringAsOutputStream(String dataString,
                                          OutputStream dataOS)
        throws IOException {
        // Set size in bytes for resData reading
        int read = 0;
        byte[] bytes = new byte[1024];

        // Get out the string
        InputStream dataIS = new ByteArrayInputStream(dataString.getBytes());
        while((read = dataIS.read(bytes))!= -1) {
            dataOS.write(bytes, 0, read);
        }
        dataOS.flush();
    }
    
    // Billy Fang
    /**
    * Performs necessary calculations and outputs results
    */
    private void callClump() {
        // Charles Malaheenee
        // "Stupids" protection
        if (fPattern == null || fPattern.length() == 0 ||
            fProb == null || fProb.length() == 0 ||
            fTruncate == null || fTruncate.length() == 0 ||
            fA == null || fA.length() == 0) {
            printError("Error: no data in one of fields");
            return;
        }
        
        fPattern = fPattern.replace("\\s{1,}", "\u0020");
        fPattern = fPattern.trim();
        fPattern = fPattern.toUpperCase();

        fProb = fProb.replace("\\s{1,}", "\u0020");
        fProb = fProb.trim();
        fProb = fProb.toUpperCase();;
       
        fTruncate = fTruncate.replace("\\s{1,}", "\u0020");
        fTruncate = fTruncate.trim();
       
        fA = fA.replace("\\s{1,}", "\u0020");
        fA = fA.trim();

        // read probabilities, check for errors
        String[] probList = fProb.split("\\s+");
        if (probList.length % 2 != 0) {
            printError("Error: must have probability for each char");
            return;
        }
     
        double[] prob = new double[R];
        double total = 0;
        for (int i = 0; i < probList.length; i+=2) {
            String letter = probList[i];
            if (letter.length() != 1) {
                printError("Error: probabilities must be for single letters.");
                return;
            }
            prob[letter.charAt(0)] = Double.parseDouble(probList[i+1]);
            total += prob[letter.charAt(0)];
        }
        if (total != 1) {
            printError("Error: probabilities must be total 1.");
            return;
        }

        // read pattern words, check for errors
        String[] words = fPattern.split("\\s+");
        int length = words[0].length();
        for (int i = 0; i < words.length; i++) {
            String thisWord = words[i];
            if (thisWord.length() != length) {
                printError("Error: pattern words must have same length.");
                return;
            }
            for (int j = 0; j < thisWord.length(); j++) {
                 char c = thisWord.charAt(j);
                 if (prob[c] == 0.0) {
                     printError(String.format("Error: letter '%c' has zero probability.", c));
                     return;
                 }
            }
        }

        // read truncation point, check for errors
        int truncate = Integer.parseInt(fTruncate);
        if (truncate <= 0) {
           printError("Error: truncation position must be positive");
           return;
        }        

        // construct AutoClump, output results and GraphViz code
        AutoClump ac = AutoClump.constructAutoClump(words, prob);
        fNumberOfStates = Integer.toString(ac.numberOfStates());
        fNumberOfEdges = Integer.toString(ac.numberOfEdges());
        aGraph = ac.printAutoClumpGraph();
        
        // construct DeepClump, output results and GraphViz code
        DeepClump dc = DeepClump.constructDeepClump(words, prob);
        fNumberOfStates2 = Integer.toString(dc.numberOfStates());
        fNumberOfEdges2  = Integer.toString(dc.numberOfEdges());
        aGraph2 = dc.printDeepClumpGraph();
        
        // get coefficients of generating function
        BigDecimal[] coef = ac.getCoefficients(truncate);

        // output coefficients of C(z)
        aPoly = "";
        for (int i = 0; i < coef.length; i++) {
            if (i != 0) {
                aPoly += "\n";
                aPoly += i + ": " + coef[i];
            }
        }

        // construct generating function
        Polynomial gf = new Polynomial(BigDecimal.ZERO, coef.length-1);
        for (int i = 0; i < coef.length; i++) {
            gf = gf.add(new Polynomial(coef[i], i));
        }

        // decimal formatting
        DecimalFormat decFormat = new DecimalFormat("0E0");
        decFormat.setMaximumFractionDigits(precision);

        // output rho, the root of 1-z+C(z), using Newton's method at z=1
        Polynomial denom = gf;
        denom = denom.add(new Polynomial(BigDecimal.ONE, 0));
        denom = denom.add(new Polynomial(BigDecimal.ONE.negate(), 1));
        BigDecimal root = denom.newton(BigDecimal.ONE, 50);
        if (root == null) {
            printError("Error: Newton's method did not converge for root of fund. equation; try smaller value of a");
            return;            
        }
    
        fRho = decFormat.format(root);

        // output 1 - C'(rho)
        BigDecimal eval2 = BigDecimal.ONE.subtract(gf.differentiate().evaluate(root));
        fEval = decFormat.format(eval2);
        
        // find root of az(C'(z)(1-z)+C(z))-C(z)(1-z+C(z))=0, using Newton's method at z=1
        Polynomial fund = new Polynomial(BigDecimal.ONE,0);
        fund = fund.add(new Polynomial(BigDecimal.ONE.negate(),1));
        fund = fund.multiply(gf.differentiate());
        fund = fund.add(gf);
        fund = fund.multiply(new Polynomial(BigDecimal.ONE, 1));
        fund = fund.multiply(new Polynomial((new BigDecimal(fA)), 0));
        fund = fund.subtract(gf.multiply(denom));
     
        BigDecimal fundRoot = fund.newton(BigDecimal.ONE, 200);
        if (fundRoot == null) {
            printError("Error: Newton's method did not converge for root of fund. equation; try smaller value of a");
            return;
        }
        else {
            fFund = decFormat.format(fundRoot);
            fStatus = "Successful";
        }
    }

    // Billy Fang
    // clear all fields and print error
    private void printError(String s) {
        fStatus = "<label style=\"color: red; text-align: center\">" + s + "</label>";
        fNumberOfStates = "";
        fNumberOfEdges = "";
        aGraph = "";
        fNumberOfStates2 = "";
        fNumberOfEdges2 = "";
        aGraph2 = "";
        fRho = "";
        aPoly = "";
        fEval = "";
        fFund = "";
    }
}

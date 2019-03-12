import org.apache.commons.cli.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;


public class stegUnicode {

    public static String UNICODE_PATH = "./confusables.txt";
    private static Options options = new Options();

    public static void main(String[] args) throws Exception{
        banner();

        CommandLine cmd = parserArguments(args);

        String inputFilePath = cmd.getOptionValue("file message");
        String text = cmd.getOptionValue("coverText");
        String hiddentext = cmd.getOptionValue("message");
        String outputFilePath = cmd.getOptionValue("output");

        String global_input = getInput(inputFilePath,text);
        if (cmd.hasOption("hidden")){
            //System.out.println("Hidden option selected");
            HashMap<String, String> m = ReadUnicodeTable(UNICODE_PATH);
            //System.out.println("Binary option selected");
            int nbits = global_input.length();

            String nhbits = getbinaryfromtexttohidden(hiddentext);
            System.out.println("\n-----");
            System.out.println("[PROCESSING] OK...");
            System.out.println("[INFO] You want to hide "+ Integer.toString(nhbits.length())+" bits");
            System.out.println("[INFO] You can hide " + Integer.toString(nbits)+" bits maximum in the covertext");
            if (nhbits.length()>nbits){
                System.out.println("Warning: It is not possible to hide the secret message. You need a bigger coverText.");
                System.exit(1);
            }
            //System.out.println(nhbits);
            String outputhidden = stegounicodebinary(global_input,nhbits,m);
            if (cmd.hasOption("output")){
                if (getFileExtension(outputFilePath).equals("pdf")){
                    writeInPdf(outputhidden,outputFilePath);
                    System.out.println("Output file with hidden info: " + outputFilePath);
                }else {
                    try (PrintWriter out = new PrintWriter(outputFilePath)) {
                        out.println(outputhidden);
                        System.out.print("[RESULT] Created stegoText <"+outputFilePath+">");

                    }
                }
            }else{
                System.out.println("[RESULT] StegoText: "+outputhidden);
            }
        }else if(cmd.hasOption("extract")){
            System.out.println("Extract option selected");
            String secret = GetSecret(global_input);
            System.out.println("Secret: ");
            StringBuilder sb = new StringBuilder(); // Some place to store the chars

            Arrays.stream( // Create a Stream
                    secret.split("(?<=\\G.{8})") // Splits the input string into 8-char-sections (Since a char has 8 bits = 1 byte)
            ).forEach(s -> // Go through each 8-char-section...
                    sb.append((char) Integer.parseInt(s, 2)) // ...and turn it into an int and then to a char
            );
            String output = sb.toString(); // Output text (t)
            System.out.println(output.replaceAll("[^\\x00-\\xFF]", "").trim());
        }else {

            //System.out.println("It is necessary to select one option: Hidden or Extract");
            System.exit(1);
        }
        System.out.println("\n");

    }


    static HashMap<String,String> ReadUnicodeTable(String pathUnicode) throws IOException {

        HashMap<String, String> map = new HashMap<>();
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(pathUnicode));
        while ((line = reader.readLine()) != null)
        {
            String[] parts = line.split("", 2);
            if (parts.length >= 2)
            {
                String key = parts[0];
                String value = parts[1];
                map.put(key, value);
            } else {
                System.out.println("ignoring line: " + line);
            }
        }
        return map;
    }

    static void banner() throws InterruptedException {
        System.out.println("" +
                "" +
                "\n" +
                "   _____ _             _    _       _               _      \n" +
                "  / ____| |           | |  | |     (_)             | |     \n" +
                " | (___ | |_ ___  __ _| |  | |_ __  _  ___ ___   __| | ___ \n" +
                "  \\___ \\| __/ _ \\/ _` | |  | | '_ \\| |/ __/ _ \\ / _` |/ _ \\\n" +
                "  ____) | ||  __/ (_| | |__| | | | | | (_| (_) | (_| |  __/\n" +
                " |_____/ \\__\\___|\\__, |\\____/|_| |_|_|\\___\\___/ \\__,_|\\___|\n" +
                "                  __/ |                                    \n" +
                "                 |___/                                     \n" +
                "\n" +
                "\n" +
                "" +
                "Version: Beta\n" +
                "Authors: Alfonso Mu\u00f1oz (@Mindcrypt) & Miguel Hern\u00e1ndez (@MiguelHzBz)\n\n" +
                "Usage: stegUnicode -f <inputfile>.txt -h -m secret -o <outputfile>.txt \n" +
                "       stegUnicode -c abcdefghijklmnopqrstuvw -h -m secret -o <outputfile>.txt \n" +
                "       stegUnicode -f <outputfile>.txt -e \n" +
                "\n");
        Thread.sleep(300);
    }

    static CommandLine parserArguments(String[] args){

        Option input = new Option("f", "file message", true, "Cover (text file) to hide the message");
        input.setRequired(false);
        options.addOption(input);

        Option text = new Option("c", "coverText", true, "Cover to hide the message");
        text.setRequired(false);
        options.addOption(text);

        Option hidtext = new Option("m", "message", true, "Text to be hidden");
        hidtext.setRequired(false);
        options.addOption(hidtext);

        Option hidden = new Option("h", "hidden", false, "Option to hide a secret message (confusables)");
        hidden.setRequired(false);
        options.addOption(hidden);

        Option extract = new Option("e", "extract", false, "Option to extract a secret message");
        extract.setRequired(false);
        options.addOption(extract);

        Option output = new Option("o", "output", true, "Output file");
        output.setRequired(false);
        options.addOption(output);

        Option paranoid = new Option("p", "paranoid", true, "Use matrix embedding and several files to reduce visual impact (in progress)");
        output.setRequired(false);
        options.addOption(paranoid);

        Option proxy = new Option("proxy", "proxy", true, "Changing info on the fly (in progress)");
        output.setRequired(false);
        options.addOption(proxy);

        Option doc = new Option("doc", "typeDoc", true, "Options stegoDocument (in progress):\n"+"-- pdf: Generate a unicodeStegoPDF from coverPDF\n"+
                "-- word: Generate a unicodeStegoMSWord from coverMSWord\n"+
                "-- html: Inyect unicodeText into HTML comments");
        output.setRequired(false);
        options.addOption(doc);

        Option tags = new Option("t", "unicodetags", true, "Insert non-printable unicode tags & Zero-width characters within the words (in progress)");
        output.setRequired(false);
        options.addOption(tags);



        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
            formatter.printHelp("stegUnicode", options);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("stegUnicode", options);
            System.exit(1);
        }
        return cmd;
    }

    static String readtxt(String filepath) throws IOException {

        Path path = FileSystems.getDefault().getPath(filepath);
        String s = path.toAbsolutePath().toString();

        File file = new File(s);

        BufferedReader br = new BufferedReader(new FileReader(file));

        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            String contentfile = sb.toString();
            return contentfile;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("File read error.");
            return "fail";
        } finally {
            br.close();
        }
    }
    public static String getFileExtension(String fileName) {
        if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        } else {
            return "";
        }
    }

    static String readFile(String file) throws IOException {
        String extension = getFileExtension(file);
        switch (extension) {
            case "txt":
                return readtxt(file);
            case "docx":
                return new String("Not yet"); //importDocx(file);
            case "html":
                return readtxt(file);
            case "pdf":
                return readpdf(file);
            default:
                return "TBD";
        }
    }

    private static String getInput(String inputFilePath, String text) throws IOException {
        if (inputFilePath == null){
            if (text==null){
                System.out.println("\nError, not input. You need to use option -f or -c.");
                System.exit(1);
            }else {
                return text;
            }
        }
        String text_file = readFile(inputFilePath);
        if (text_file == "TBD"){
            System.out.println("En desarrollo");
            System.exit(1);
        }
        return text_file;
    }


    private static void writeInPdf(String outputhidden, String s) throws IOException {

        //Creating PDF document object
        PDDocument document = new PDDocument();
        PDPage blankPage = new PDPage();

        PDPageContentStream contentStream = new PDPageContentStream(document, blankPage);
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 12);
        try {
            contentStream.showText(outputhidden);
            contentStream.endText();
            //Saving the document
            document.save(s);

            System.out.println("PDF created");
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
        //Closing the document
        document.close();
    }

    public static String GetSecret(String ascii) {
        String ascii_clean = Clean(ascii);
        String aux = "";
        String[] ascii_split = ascii_clean.split("");
        for(int i=0;i<ascii_split.length;i++)
        {
            if ((int)ascii_split[i].charAt(0) < 128){
                aux += "0";
            }else{
                aux+= "1";
            }
            if ((int)ascii_split[i].charAt(0) > 55000){
                i++;
            }
        }
        System.out.println(aux);
        return aux;
    }

    private static String Clean(String ascii) {
        String aux = ascii.replaceAll("t","").replaceAll("F","").replaceAll("í","").replaceAll("L","").replaceAll("m","").replaceAll("r","").replaceAll("t","").replaceAll("R","").replaceAll(" ","");
        System.out.println(aux);
        return aux;
    }

    public static String stegounicodebinary(String text, String nhbits, HashMap<String, String> m) {
        String aux = "";
        int aux_index = 0;
        String[] split_text = text.split("");
        String[] splitbits = nhbits.split("");
        for (int i=0;i<splitbits.length;i++){
            while (!m.containsKey(split_text[aux_index])){
                aux += split_text[aux_index];
                aux_index++;
            }
            if (splitbits[i].contains("1")) {
                aux += m.get(split_text[aux_index]).toString();
            }else{
                aux += split_text[aux_index];
            }
            aux_index++;
        }
        aux += text.substring(splitbits.length,text.length());
        return aux;
    }

    public static String getbinaryfromtexttohidden(String hiddentext) {
        byte[] bytes = hiddentext.getBytes();
        StringBuilder binary = new StringBuilder();
        for (byte b : bytes)
        {
            int val = b;
            for (int i = 0; i < 8; i++)
            {
                binary.append((val & 128) == 0 ? 0 : 1);
                val <<= 1;
            }
        }
        return binary.toString();
    }
    public static String readpdf(String f) throws IOException {

        File file = new File(f);
        PDDocument document = PDDocument.load(file);

        //Instantiate PDFTextStripper class
        PDFTextStripper pdfStripper = new PDFTextStripper();

        //Retrieving text from PDF document
        String text = pdfStripper.getText(document);
        System.out.println(text);

        //Closing the document
        document.close();
        return text;
    }
}
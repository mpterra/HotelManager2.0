package controller;

import org.apache.poi.xwpf.usermodel.*;
import javax.swing.*;
import java.awt.Component;
import java.io.*;
import java.util.Map;

public class GerarContrato {

    // Recebe o componente pai para o diálogo
	public static void gerarComDialogo(Component parent, Map<String, String> dados) {
	    JFileChooser fileChooser = new JFileChooser();
	    fileChooser.setDialogTitle("Salvar Contrato");

	    String id = dados.getOrDefault("{{reserva_id}}", "0");
	    String ano = dados.getOrDefault("{{ano}}", "0000");
	    String nome = dados.getOrDefault("{{nome}}", "nome").replaceAll("[^a-zA-Z0-9]", "_");

	    String nomeArquivo = String.format("Termo %s %s %s.docx", id, ano, nome);
	    fileChooser.setSelectedFile(new File(nomeArquivo));

	    int userSelection = fileChooser.showSaveDialog(parent);

	    if (userSelection == JFileChooser.APPROVE_OPTION) {
	        File arquivoDestino = fileChooser.getSelectedFile();

	        // Tenta abrir o modelo embutido no JAR
	        try (InputStream inputStream = GerarContrato.class.getResourceAsStream("/resources/modelos/modelo_reserva.docx")) {
	            if (inputStream == null) {
	                throw new IllegalStateException("Modelo não encontrado dentro do JAR!");
	            }

	            // Cria arquivo temporário para manipulação
	            File tempFile = File.createTempFile("modelo_reserva", ".docx");
	            tempFile.deleteOnExit(); // será apagado quando o programa fechar

	            try (FileOutputStream out = new FileOutputStream(tempFile)) {
	                inputStream.transferTo(out);
	            }

	            // Abre o arquivo temporário com Apache POI
	            try (XWPFDocument document = new XWPFDocument(new FileInputStream(tempFile))) {

	                // Substitui texto e aplica fonte nos parágrafos
	                for (XWPFParagraph p : document.getParagraphs()) {
	                    substituirTexto(p, dados);
	                    aplicarFonte(p);
	                }

	                // Substitui texto e aplica fonte nas tabelas
	                for (XWPFTable table : document.getTables()) {
	                    for (XWPFTableRow row : table.getRows()) {
	                        for (XWPFTableCell cell : row.getTableCells()) {
	                            for (XWPFParagraph p : cell.getParagraphs()) {
	                                substituirTexto(p, dados);
	                                aplicarFonte(p);
	                            }
	                        }
	                    }
	                }

	                // Salva no local escolhido pelo usuário
	                try (FileOutputStream fos = new FileOutputStream(arquivoDestino)) {
	                    document.write(fos);
	                }

	                JOptionPane.showMessageDialog(parent,
	                        "Contrato salvo com sucesso em:\n" + arquivoDestino.getAbsolutePath());

	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	            JOptionPane.showMessageDialog(parent, "Erro ao gerar contrato:\n" + e.getMessage());
	        }
	    } else {
	        System.out.println("Usuário cancelou o salvamento.");
	    }
	}



    private static void substituirTexto(XWPFParagraph paragraph, Map<String, String> dados) {
        StringBuilder paragraphText = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            String texto = run.getText(0);
            if (texto != null) {
                paragraphText.append(texto);
            }
        }

        String textoCompleto = paragraphText.toString();
        String textoModificado = textoCompleto;

        for (Map.Entry<String, String> entry : dados.entrySet()) {
            textoModificado = textoModificado.replace(entry.getKey(), entry.getValue());
        }

        if (!textoCompleto.equals(textoModificado)) {
            int runsCount = paragraph.getRuns().size();
            for (int i = runsCount - 1; i >= 0; i--) {
                paragraph.removeRun(i);
            }
            XWPFRun novoRun = paragraph.createRun();
            novoRun.setText(textoModificado);
        }
    }

    private static void aplicarFonte(XWPFParagraph paragraph) {
        for (XWPFRun run : paragraph.getRuns()) {
            run.setFontFamily("Lato");
            run.setFontSize(12);
        }
    }
}

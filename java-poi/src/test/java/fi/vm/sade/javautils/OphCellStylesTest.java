package fi.vm.sade.javautils;

import static org.apache.poi.ss.usermodel.HorizontalAlignment.GENERAL;
import static org.apache.poi.ss.usermodel.HorizontalAlignment.LEFT;

import fi.vm.sade.javautils.poi.OphCellStyles;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.Assert;
import org.junit.Test;

public class OphCellStylesTest {
    private HSSFWorkbook workbook = new HSSFWorkbook();
    private OphCellStyles.OphHssfCellStyles cellStyles = new OphCellStyles.OphHssfCellStyles(workbook);
    private HSSFSheet sheet = workbook.createSheet();
    private HSSFRow row = sheet.createRow(1);
    private HSSFCell cell = row.createCell(1);

    @Test
    public void cellsWithDangerousContentGetQuotePrefixes() {
        cell.setCellValue("=1+2");
        cellStyles.apply(cell);
        Assert.assertTrue(cell.getCellStyle().getQuotePrefixed());

        cell.setCellValue("@[1]");
        cellStyles.apply(cell);
        Assert.assertTrue(cell.getCellStyle().getQuotePrefixed());

        cell.setCellValue("+1");
        cellStyles.apply(cell);
        Assert.assertTrue(cell.getCellStyle().getQuotePrefixed());

        cell.setCellValue("-1");
        cellStyles.apply(cell);
        Assert.assertTrue(cell.getCellStyle().getQuotePrefixed());
    }

    @Test
    public void cellsWithPlainContentDoNotGetQuotePrefixes() {
        cell.setCellValue("1");
        cellStyles.apply(cell);
        Assert.assertFalse(cell.getCellStyle().getQuotePrefixed());

        cell.setCellValue("Dog");
        cellStyles.apply(cell);
        Assert.assertFalse(cell.getCellStyle().getQuotePrefixed());

        cell.setCellValue("/hello");
        cellStyles.apply(cell);
        Assert.assertFalse(cell.getCellStyle().getQuotePrefixed());
    }

    @Test
    public void propertiesCanBeSetToBothStylesAtSameTime() {
        HSSFCell safeCell = row.createCell(2);
        safeCell.setCellValue("Hello");
        HSSFCell dangerousCell = row.createCell(3);
        dangerousCell.setCellValue("=1+2");

        Assert.assertEquals(GENERAL, safeCell.getCellStyle().getAlignmentEnum());
        Assert.assertEquals(GENERAL, dangerousCell.getCellStyle().getAlignmentEnum());

        cellStyles.visit(s -> s.setAlignment(LEFT));

        cellStyles.apply(safeCell);
        cellStyles.apply(dangerousCell);

        Assert.assertEquals(LEFT, safeCell.getCellStyle().getAlignmentEnum());
        Assert.assertEquals(LEFT, dangerousCell.getCellStyle().getAlignmentEnum());
    }
}

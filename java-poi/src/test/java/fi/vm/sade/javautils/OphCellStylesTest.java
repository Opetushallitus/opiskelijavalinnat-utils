package fi.vm.sade.javautils;

import fi.vm.sade.javautils.poi.OphCellStyles;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.Assert;
import org.junit.Test;

import static org.apache.poi.ss.usermodel.HorizontalAlignment.*;

public class OphCellStylesTest {
    private HSSFWorkbook workbook = new HSSFWorkbook();
    private OphCellStyles cellStyles = new OphCellStyles(workbook);
    private HSSFSheet sheet = workbook.createSheet();
    private HSSFRow row = sheet.createRow(1);

    @Test
    public void cellsWithDangerousContentGetQuotePrefixes() {
        HSSFCell cell = row.createCell(1);
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
        HSSFCell cell = row.createCell(1);
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

    @Test
    public void numericAndOtherNonTextCellsUseUnsafeStyle() {
        cellStyles.visit(s -> s.setAlignment(LEFT));
        HSSFCell cell = row.createCell(2);

        cell.setCellType(CellType.NUMERIC);
        cellStyles.apply(cell);
        Assert.assertEquals(LEFT, cell.getCellStyle().getAlignmentEnum());
        Assert.assertFalse(cell.getCellStyle().getQuotePrefixed());

        cell.setCellType(CellType.BOOLEAN);
        cellStyles.apply(cell);
        Assert.assertEquals(LEFT, cell.getCellStyle().getAlignmentEnum());
        Assert.assertFalse(cell.getCellStyle().getQuotePrefixed());

        cell.setCellType(CellType.BLANK);
        cellStyles.apply(cell);
        Assert.assertEquals(LEFT, cell.getCellStyle().getAlignmentEnum());
        Assert.assertFalse(cell.getCellStyle().getQuotePrefixed());

        cell.setCellType(CellType.ERROR);
        cellStyles.apply(cell);
        Assert.assertEquals(LEFT, cell.getCellStyle().getAlignmentEnum());
        Assert.assertFalse(cell.getCellStyle().getQuotePrefixed());
    }

    @Test
    public void settingRowStyleDoesNotOverrideSingleCellStyles() {
        cellStyles.visit(s -> s.setAlignment(LEFT));
        HSSFCell cell = row.createCell(2);
        cellStyles.apply(cell);

        OphCellStyles rowStyles = new OphCellStyles(workbook);
        rowStyles.visit(rs -> rs.setAlignment(RIGHT));
        rowStyles.apply(row);

        Assert.assertEquals(LEFT, cell.getCellStyle().getAlignmentEnum());

        HSSFCell cell2 = row.createCell(2);
        Assert.assertEquals(GENERAL, cell2.getCellStyle().getAlignmentEnum());
    }

    @Test(expected = IllegalArgumentException.class)
    public void creatingFormulaCellsIsNotSupported() {
        HSSFCell cell = row.createCell(2);
        cell.setCellType(CellType.FORMULA);
        cellStyles.apply(cell);
    }
}

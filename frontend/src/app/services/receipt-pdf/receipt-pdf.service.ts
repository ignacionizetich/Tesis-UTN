import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';
import Transaction from '../../models/transaction';
import { formatMoney } from '../../shared/utils/money-format';
import { formatDateTimeDetailed } from '../../shared/utils/date-format';

export interface ReceiptLabels {
  title: string;
  description: string;
  amountLabel: string;
  origin: string;
  destination: string;
}

@Injectable({
  providedIn: 'root',
})
export class ReceiptPdfService {
  buildPdf(tx: Transaction, labels: ReceiptLabels): jsPDF {
    const doc = new jsPDF({ unit: 'pt', format: 'a4' });
    const pageW = doc.internal.pageSize.getWidth();
    const marginX = 48;
    let y = 56;

    // Header brand
    doc.setFillColor(10, 102, 255);
    doc.rect(0, 0, pageW, 88, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(22);
    doc.text('Arcash', marginX, 42);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(11);
    doc.text('Comprobante de operación', marginX, 64);

    y = 120;
    doc.setTextColor(15, 23, 42);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(16);
    doc.text(labels.title, marginX, y);

    y += 22;
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(11);
    doc.setTextColor(71, 85, 105);
    doc.text(labels.description, marginX, y, { maxWidth: pageW - marginX * 2 });

    y += 36;
    doc.setFillColor(248, 250, 252);
    doc.roundedRect(marginX, y, pageW - marginX * 2, 64, 8, 8, 'F');
    doc.setTextColor(15, 23, 42);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(20);
    doc.text(labels.amountLabel, marginX + 16, y + 40);

    y += 92;
    doc.setDrawColor(226, 232, 240);
    doc.setLineWidth(1);

    const rows: Array<[string, string]> = [
      ['Fecha', formatDateTimeDetailed(tx.date)],
      ['Estado', tx.status === 'FAILED' ? 'Fallida' : 'Completada'],
      ['Tipo', this.kindLabel(tx)],
      ['Moneda', tx.currency || 'ARS'],
    ];

    if (tx.kind === 'transfer' || tx.kind === 'buy_usd' || tx.kind === 'sell_usd') {
      rows.push(['Origen', labels.origin]);
      rows.push(['Destino', labels.destination]);
    }
    if (tx.exchangeRate) {
      rows.push(['Cotización', `$${formatMoney(tx.exchangeRate)} ARS`]);
    }
    if (tx.taxAmount != null && tx.taxAmount > 0) {
      const pct = tx.taxPercentage ? ` (${tx.taxPercentage}%)` : '';
      rows.push([`Comisión${pct}`, `$${formatMoney(tx.taxAmount)}`]);
    }
    if (tx.idOperation) {
      rows.push(['ID operación', tx.idOperation]);
    }
    rows.push(['ID interno', String(tx.id)]);

    doc.setFontSize(10);
    for (const [label, value] of rows) {
      if (y > 760) {
        doc.addPage();
        y = 56;
      }
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(100, 116, 139);
      doc.text(label, marginX, y);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(15, 23, 42);
      const lines = doc.splitTextToSize(value, pageW - marginX * 2 - 140);
      doc.text(lines, marginX + 140, y);
      const blockH = Math.max(16, lines.length * 13);
      y += blockH + 10;
      doc.setDrawColor(241, 245, 249);
      doc.line(marginX, y - 6, pageW - marginX, y - 6);
    }

    y += 24;
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(148, 163, 184);
    doc.text(
      'Comprobante generado por Arcash. Documento informativo de la operación en la billetera.',
      marginX,
      y,
      { maxWidth: pageW - marginX * 2 }
    );

    return doc;
  }

  filename(tx: Transaction): string {
    const code = tx.currency || 'ARS';
    const id = tx.idOperation || String(tx.id);
    return `arcash-comprobante-${code}-${id}.pdf`;
  }

  async download(tx: Transaction, labels: ReceiptLabels): Promise<void> {
    const doc = this.buildPdf(tx, labels);
    doc.save(this.filename(tx));
  }

  async share(tx: Transaction, labels: ReceiptLabels): Promise<'shared' | 'downloaded'> {
    const doc = this.buildPdf(tx, labels);
    const blob = doc.output('blob');
    const file = new File([blob], this.filename(tx), { type: 'application/pdf' });

    const canShareFiles =
      typeof navigator !== 'undefined' &&
      typeof navigator.share === 'function' &&
      (!navigator.canShare || navigator.canShare({ files: [file] }));

    if (canShareFiles) {
      try {
        await navigator.share({
          title: 'Comprobante Arcash',
          text: labels.description,
          files: [file],
        });
        return 'shared';
      } catch (error: unknown) {
        // Usuario canceló el sheet nativo
        if (error instanceof DOMException && error.name === 'AbortError') {
          throw error;
        }
      }
    }

    doc.save(this.filename(tx));
    return 'downloaded';
  }

  private kindLabel(tx: Transaction): string {
    if (tx.kind === 'buy_usd') return 'Compra de dólares';
    if (tx.kind === 'sell_usd') return 'Venta de dólares';
    if (tx.kind === 'loan_credit') return 'Acreditación de préstamo';
    if (tx.kind === 'loan_payment') return 'Pago de cuota de préstamo';
    return tx.type === 'income' ? 'Transferencia recibida' : 'Transferencia enviada';
  }
}

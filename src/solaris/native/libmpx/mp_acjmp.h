/*
 * @(#)mp_acjmp.h	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

case 0:
   ps += 1;
   cf = base[0][ps];
   zzrmbits += 6;
   goto L_SingleCoef;
case 1:
   ps += 1;
   cf = -base[0][ps];
   zzrmbits += 6;
   goto L_SingleCoef;
case 2:
   ps += 2;
   cf = base[0][ps];
   zzrmbits += 5;
   goto L_SingleCoef;
case 3:
   ps += 2;
   cf = -base[0][ps];
   zzrmbits += 5;
   goto L_SingleCoef;
case 4:
   ps += 1;
   cf = base[1][ps];
   zzrmbits += 4;
   goto L_SingleCoef;
case 5:
   ps += 1;
   cf = -base[1][ps];
   zzrmbits += 4;
   goto L_SingleCoef;
case 6:
   ps += 3;
   cf = base[0][ps];
   zzrmbits += 4;
   goto L_SingleCoef;
case 7:
   ps += 3;
   cf = -base[0][ps];
   zzrmbits += 4;
   goto L_SingleCoef;
case 8:
   ps += 1;
   cf = base[2][ps];
   zzrmbits += 3;
   goto L_SingleCoef;
case 9:
   ps += 1;
   cf = -base[2][ps];
   zzrmbits += 3;
   goto L_SingleCoef;
case 10:
   ps += 4;
   cf = base[0][ps];
   zzrmbits += 3;
   goto L_SingleCoef;
case 11:
   ps += 4;
   cf = -base[0][ps];
   zzrmbits += 3;
   goto L_SingleCoef;
case 12:
   ps += 5;
   cf = base[0][ps];
   zzrmbits += 3;
   goto L_SingleCoef;
case 13:
   ps += 5;
   cf = -base[0][ps];
   zzrmbits += 3;
   goto L_SingleCoef;
case 14:
   ps += 2;
   cf = base[1][ps];
   zzrmbits += 2;
   goto L_SingleCoef;
case 15:
   ps += 2;
   cf = -base[1][ps];
   zzrmbits += 2;
   goto L_SingleCoef;
case 16:
   ps += 6;
   cf = base[0][ps];
   zzrmbits += 2;
   goto L_SingleCoef;
case 17:
   ps += 6;
   cf = -base[0][ps];
   zzrmbits += 2;
   goto L_SingleCoef;
case 18:
   ps += 7;
   cf = base[0][ps];
   zzrmbits += 2;
   goto L_SingleCoef;
case 19:
   ps += 7;
   cf = -base[0][ps];
   zzrmbits += 2;
   goto L_SingleCoef;
case 20:
   ps += 8;
   cf = base[0][ps];
   zzrmbits += 2;
   goto L_SingleCoef;
case 21:
   ps += 8;
   cf = -base[0][ps];
   zzrmbits += 2;
   goto L_SingleCoef;
case 22:
   ps += 1;
   cf = base[3][ps];
   zzrmbits += 1;
   goto L_SingleCoef;
case 23:
   ps += 1;
   cf = -base[3][ps];
   zzrmbits += 1;
   goto L_SingleCoef;
case 24:
   ps += 3;
   cf = base[1][ps];
   zzrmbits += 1;
   goto L_SingleCoef;
case 25:
   ps += 3;
   cf = -base[1][ps];
   zzrmbits += 1;
   goto L_SingleCoef;
case 26:
   ps += 9;
   cf = base[0][ps];
   zzrmbits += 1;
   goto L_SingleCoef;
case 27:
   ps += 9;
   cf = -base[0][ps];
   zzrmbits += 1;
   goto L_SingleCoef;
case 28:
   ps += 10;
   cf = base[0][ps];
   zzrmbits += 1;
   goto L_SingleCoef;
case 29:
   ps += 10;
   cf = -base[0][ps];
   zzrmbits += 1;
   goto L_SingleCoef;
case 30:
   ps += 1;
   cf = base[4][ps];
   zzrmbits += 0;
   goto L_SingleCoef;
case 31:
   ps += 1;
   cf = -base[4][ps];
   zzrmbits += 0;
   goto L_SingleCoef;
case 32:
   ps += 1;
   cf = base[5][ps];
   zzrmbits += 0;
   goto L_SingleCoef;
case 33:
   ps += 1;
   cf = -base[5][ps];
   zzrmbits += 0;
   goto L_SingleCoef;
case 34:
   ps += 2;
   cf = base[2][ps];
   zzrmbits += 0;
   goto L_SingleCoef;
case 35:
   ps += 2;
   cf = -base[2][ps];
   zzrmbits += 0;
   goto L_SingleCoef;
case 36:
   ps += 4;
   cf = base[1][ps];
   zzrmbits += 0;
   goto L_SingleCoef;
case 37:
   ps += 4;
   cf = -base[1][ps];
   zzrmbits += 0;
   goto L_SingleCoef;
case 38:
   ps += 11;
   cf = base[0][ps];
   zzrmbits += 0;
   goto L_SingleCoef;
case 39:
   ps += 11;
   cf = -base[0][ps];
   zzrmbits += 0;
   goto L_SingleCoef;
case 40:
   ps += 12;
   cf = base[0][ps];
   zzrmbits += 0;
   goto L_SingleCoef;
case 41:
   ps += 12;
   cf = -base[0][ps];
   zzrmbits += 0;
   goto L_SingleCoef;
case 42:
   ps += 13;
   cf = base[0][ps];
   zzrmbits += 0;
   goto L_SingleCoef;
case 43:
   ps += 13;
   cf = -base[0][ps];
   zzrmbits += 0;
   goto L_SingleCoef;
case 44:
   ps += 14;
   cf = base[0][ps];
   zzrmbits += 0;
   goto L_SingleCoef;
case 45:
   ps += 14;
   cf = -base[0][ps];
   zzrmbits += 0;
   goto L_SingleCoef;
case 46:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 3;
   goto L_DoubleCoef;
case 47:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 3;
   goto L_DoubleCoef;
case 48:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 3;
   goto L_DoubleCoef;
case 49:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 3;
   goto L_DoubleCoef;
case 50:
   ps0 = ps + 1;
   ps = ps0 + 2;
   cf0 = base[0][ps0];
   cf = base[0][ps0+2];
   zzrmbits += 2;
   goto L_DoubleCoef;
case 51:
   ps0 = ps + 1;
   ps = ps0 + 2;
   cf0 = base[0][ps0];
   cf = -base[0][ps0+2];
   zzrmbits += 2;
   goto L_DoubleCoef;
case 52:
   ps0 = ps + 1;
   ps = ps0 + 2;
   cf0 = -base[0][ps0];
   cf = base[0][ps0+2];
   zzrmbits += 2;
   goto L_DoubleCoef;
case 53:
   ps0 = ps + 1;
   ps = ps0 + 2;
   cf0 = -base[0][ps0];
   cf = -base[0][ps0+2];
   zzrmbits += 2;
   goto L_DoubleCoef;
case 54:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = base[1][ps0+1];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 55:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = -base[1][ps0+1];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 56:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = base[1][ps0+1];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 57:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = -base[1][ps0+1];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 58:
   ps0 = ps + 1;
   ps = ps0 + 3;
   cf0 = base[0][ps0];
   cf = base[0][ps0+3];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 59:
   ps0 = ps + 1;
   ps = ps0 + 3;
   cf0 = base[0][ps0];
   cf = -base[0][ps0+3];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 60:
   ps0 = ps + 1;
   ps = ps0 + 3;
   cf0 = -base[0][ps0];
   cf = base[0][ps0+3];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 61:
   ps0 = ps + 1;
   ps = ps0 + 3;
   cf0 = -base[0][ps0];
   cf = -base[0][ps0+3];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 62:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = base[2][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 63:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = -base[2][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 64:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = base[2][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 65:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = -base[2][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 66:
   ps0 = ps + 1;
   ps = ps0 + 4;
   cf0 = base[0][ps0];
   cf = base[0][ps0+4];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 67:
   ps0 = ps + 1;
   ps = ps0 + 4;
   cf0 = base[0][ps0];
   cf = -base[0][ps0+4];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 68:
   ps0 = ps + 1;
   ps = ps0 + 4;
   cf0 = -base[0][ps0];
   cf = base[0][ps0+4];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 69:
   ps0 = ps + 1;
   ps = ps0 + 4;
   cf0 = -base[0][ps0];
   cf = -base[0][ps0+4];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 70:
   ps0 = ps + 1;
   ps = ps0 + 5;
   cf0 = base[0][ps0];
   cf = base[0][ps0+5];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 71:
   ps0 = ps + 1;
   ps = ps0 + 5;
   cf0 = base[0][ps0];
   cf = -base[0][ps0+5];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 72:
   ps0 = ps + 1;
   ps = ps0 + 5;
   cf0 = -base[0][ps0];
   cf = base[0][ps0+5];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 73:
   ps0 = ps + 1;
   ps = ps0 + 5;
   cf0 = -base[0][ps0];
   cf = -base[0][ps0+5];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 74:
   ps0 = ps + 2;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 2;
   goto L_DoubleCoef;
case 75:
   ps0 = ps + 2;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 2;
   goto L_DoubleCoef;
case 76:
   ps0 = ps + 2;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 2;
   goto L_DoubleCoef;
case 77:
   ps0 = ps + 2;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 2;
   goto L_DoubleCoef;
case 78:
   ps0 = ps + 2;
   ps = ps0 + 2;
   cf0 = base[0][ps0];
   cf = base[0][ps0+2];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 79:
   ps0 = ps + 2;
   ps = ps0 + 2;
   cf0 = base[0][ps0];
   cf = -base[0][ps0+2];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 80:
   ps0 = ps + 2;
   ps = ps0 + 2;
   cf0 = -base[0][ps0];
   cf = base[0][ps0+2];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 81:
   ps0 = ps + 2;
   ps = ps0 + 2;
   cf0 = -base[0][ps0];
   cf = -base[0][ps0+2];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 82:
   ps0 = ps + 2;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = base[1][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 83:
   ps0 = ps + 2;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = -base[1][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 84:
   ps0 = ps + 2;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = base[1][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 85:
   ps0 = ps + 2;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = -base[1][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 86:
   ps0 = ps + 2;
   ps = ps0 + 3;
   cf0 = base[0][ps0];
   cf = base[0][ps0+3];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 87:
   ps0 = ps + 2;
   ps = ps0 + 3;
   cf0 = base[0][ps0];
   cf = -base[0][ps0+3];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 88:
   ps0 = ps + 2;
   ps = ps0 + 3;
   cf0 = -base[0][ps0];
   cf = base[0][ps0+3];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 89:
   ps0 = ps + 2;
   ps = ps0 + 3;
   cf0 = -base[0][ps0];
   cf = -base[0][ps0+3];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 90:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = base[1][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 91:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = base[1][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 92:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = -base[1][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 93:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = -base[1][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 94:
   ps0 = ps + 1;
   ps = ps0 + 2;
   cf0 = base[1][ps0];
   cf = base[0][ps0+2];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 95:
   ps0 = ps + 1;
   ps = ps0 + 2;
   cf0 = base[1][ps0];
   cf = -base[0][ps0+2];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 96:
   ps0 = ps + 1;
   ps = ps0 + 2;
   cf0 = -base[1][ps0];
   cf = base[0][ps0+2];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 97:
   ps0 = ps + 1;
   ps = ps0 + 2;
   cf0 = -base[1][ps0];
   cf = -base[0][ps0+2];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 98:
   ps0 = ps + 3;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 99:
   ps0 = ps + 3;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 100:
   ps0 = ps + 3;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 101:
   ps0 = ps + 3;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 1;
   goto L_DoubleCoef;
case 102:
   ps0 = ps + 3;
   ps = ps0 + 2;
   cf0 = base[0][ps0];
   cf = base[0][ps0+2];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 103:
   ps0 = ps + 3;
   ps = ps0 + 2;
   cf0 = base[0][ps0];
   cf = -base[0][ps0+2];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 104:
   ps0 = ps + 3;
   ps = ps0 + 2;
   cf0 = -base[0][ps0];
   cf = base[0][ps0+2];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 105:
   ps0 = ps + 3;
   ps = ps0 + 2;
   cf0 = -base[0][ps0];
   cf = -base[0][ps0+2];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 106:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = base[2][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 107:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = base[2][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 108:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = -base[2][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 109:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = -base[2][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 110:
   ps0 = ps + 4;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 111:
   ps0 = ps + 4;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 112:
   ps0 = ps + 4;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 113:
   ps0 = ps + 4;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 114:
   ps0 = ps + 5;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 115:
   ps0 = ps + 5;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 116:
   ps0 = ps + 5;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 117:
   ps0 = ps + 5;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoef;
case 118:
   ps += 1;
   cf = base[0][ps];
   zzrmbits += 4;
   goto L_SingleCoefEOB;
case 119:
   ps += 1;
   cf = -base[0][ps];
   zzrmbits += 4;
   goto L_SingleCoefEOB;
case 120:
   ps += 2;
   cf = base[0][ps];
   zzrmbits += 3;
   goto L_SingleCoefEOB;
case 121:
   ps += 2;
   cf = -base[0][ps];
   zzrmbits += 3;
   goto L_SingleCoefEOB;
case 122:
   ps += 1;
   cf = base[1][ps];
   zzrmbits += 2;
   goto L_SingleCoefEOB;
case 123:
   ps += 1;
   cf = -base[1][ps];
   zzrmbits += 2;
   goto L_SingleCoefEOB;
case 124:
   ps += 3;
   cf = base[0][ps];
   zzrmbits += 2;
   goto L_SingleCoefEOB;
case 125:
   ps += 3;
   cf = -base[0][ps];
   zzrmbits += 2;
   goto L_SingleCoefEOB;
case 126:
   ps += 1;
   cf = base[2][ps];
   zzrmbits += 1;
   goto L_SingleCoefEOB;
case 127:
   ps += 1;
   cf = -base[2][ps];
   zzrmbits += 1;
   goto L_SingleCoefEOB;
case 128:
   ps += 4;
   cf = base[0][ps];
   zzrmbits += 1;
   goto L_SingleCoefEOB;
case 129:
   ps += 4;
   cf = -base[0][ps];
   zzrmbits += 1;
   goto L_SingleCoefEOB;
case 130:
   ps += 5;
   cf = base[0][ps];
   zzrmbits += 1;
   goto L_SingleCoefEOB;
case 131:
   ps += 5;
   cf = -base[0][ps];
   zzrmbits += 1;
   goto L_SingleCoefEOB;
case 132:
   ps += 2;
   cf = base[1][ps];
   zzrmbits += 0;
   goto L_SingleCoefEOB;
case 133:
   ps += 2;
   cf = -base[1][ps];
   zzrmbits += 0;
   goto L_SingleCoefEOB;
case 134:
   ps += 6;
   cf = base[0][ps];
   zzrmbits += 0;
   goto L_SingleCoefEOB;
case 135:
   ps += 6;
   cf = -base[0][ps];
   zzrmbits += 0;
   goto L_SingleCoefEOB;
case 136:
   ps += 7;
   cf = base[0][ps];
   zzrmbits += 0;
   goto L_SingleCoefEOB;
case 137:
   ps += 7;
   cf = -base[0][ps];
   zzrmbits += 0;
   goto L_SingleCoefEOB;
case 138:
   ps += 8;
   cf = base[0][ps];
   zzrmbits += 0;
   goto L_SingleCoefEOB;
case 139:
   ps += 8;
   cf = -base[0][ps];
   zzrmbits += 0;
   goto L_SingleCoefEOB;
case 140:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 1;
   goto L_DoubleCoefEOB;
case 141:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 1;
   goto L_DoubleCoefEOB;
case 142:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 1;
   goto L_DoubleCoefEOB;
case 143:
   ps0 = ps + 1;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 1;
   goto L_DoubleCoefEOB;
case 144:
   ps0 = ps + 1;
   ps = ps0 + 2;
   cf0 = base[0][ps0];
   cf = base[0][ps0+2];
   zzrmbits += 0;
   goto L_DoubleCoefEOB;
case 145:
   ps0 = ps + 1;
   ps = ps0 + 2;
   cf0 = base[0][ps0];
   cf = -base[0][ps0+2];
   zzrmbits += 0;
   goto L_DoubleCoefEOB;
case 146:
   ps0 = ps + 1;
   ps = ps0 + 2;
   cf0 = -base[0][ps0];
   cf = base[0][ps0+2];
   zzrmbits += 0;
   goto L_DoubleCoefEOB;
case 147:
   ps0 = ps + 1;
   ps = ps0 + 2;
   cf0 = -base[0][ps0];
   cf = -base[0][ps0+2];
   zzrmbits += 0;
   goto L_DoubleCoefEOB;
case 148:
   ps0 = ps + 2;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoefEOB;
case 149:
   ps0 = ps + 2;
   ps = ps0 + 1;
   cf0 = base[0][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoefEOB;
case 150:
   ps0 = ps + 2;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = base[0][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoefEOB;
case 151:
   ps0 = ps + 2;
   ps = ps0 + 1;
   cf0 = -base[0][ps0];
   cf = -base[0][ps0+1];
   zzrmbits += 0;
   goto L_DoubleCoefEOB;

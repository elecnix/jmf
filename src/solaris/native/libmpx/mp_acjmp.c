/*
 * @(#)mp_acjmp.c	1.2 02/08/21
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

#include "stdio.h"
typedef  unsigned char 	ubyte;
typedef  int nint;

/* Constant data array which is used to initialize dvlc_tcf[]
   There are 31 entries of 4 values each. These correspond to
   [run, absolute level, lenght of the vlc for this pair excluding 
    the sign bit, value of the vlc excluding the sign bit]
   The entry addresses 0-30 will be the indices returned by dvlc_tcf[]
   corresponding to these run,level pairs.
*/
const ubyte rlev[31][4] = 
      {	 0,1,2,3,  1,1,3,3,   0,2,4,4,  2,1,4,5,   0,3,5,5,   3,1,5,7,
      	 4,1,5,6,  1,2,6,6,  5,1,6,7,  6,1,6,5,  7,1,6,4,  0,4,7,6,
      	 2,2,7,4,  8,1,7,7,  9,1,7,5,  0,5,8,38,  0,6,8,33,  1,3,8,37,
      	 3,2,8,36,  10,1,8,39,  11,1,8,35,  12,1,8,34,  13,1,8,32,  
      	 0,7,10,10,   1,4,10,12,   2,3,10,11,   4,2,10,15,  5,2,10,9,
         14,1,10,14,  15,1,10,13,   16,1,10,8  };


/* Each entry is an index into the rlev[31][4] table */
const ubyte dbl[18][2] = 
   {  0,0, 0,1, 0,2, 0,3, 0,4, 0,5, 0,6,
      1,0, 1,1, 1,2, 1,3,
      2,0, 2,1,
      3,0, 3,1,
      4,0,
      5,0,
      6,0
   };
   
const ubyte dbl_eob[3][2] = 
   {  0,0, 0,1, 1,0 };
   



ubyte jmp_ac9dec[512];
ubyte lk_chracskip[512];

/****************************************************************************
   CASE NUMBERING:
   
   Single coeff: [0,45]
      Odd entries are for negative levels. case# / 2 points to the rlev[]
      table. rlev[0-22] correspond to run,level VLCs of length <=9.
   
   Double coeff: [46,117]
      There are 18 possible double <run,abs level> VLCs of length <= 9.
      (Case# - 46) / 4 points to a dbl[] entry. 
      For each possible double <run, abs level> there 4 cases in sequence,
      corresponding to (+,+) (+,-) (-,+) and (-,-)
   
   Single coeff. followed by EOB: [118,139]
      Since VLC for EOB is "10", size of the VLC preceeding the EOB is <=7
      including the sign bit. So the VLC preceeding the EOB corresponds
      to one of the first 11 entries in rlev[].
      (case# - 118)/2 points to rlev[].  

   Double coeff. followed by EOB: [140,151]
      (case# - 140)/4 points to dbl_eob[], entries of which in turn point to
      rlev[].
      For each possible double coeff. + EOB,there 4 cases in sequence,
      corresponding to (+,+) (+,-) (-,+) and (-,-)
   
   "0000001xx" : [152]
   
   "00000001x" : [153]
   
   "000000001" : [154]
   
   "000000000" : [155]
   
   EOB:	 [156]
   ESC:	 [157]


****************************************************************************/



main()
{  register int k,m,n,s1,s2,v1,v2;
   register unsigned int  w;
   
   /* Initialize all the SINGLE COEFF. entries. Some of these will be 
      overwritten during the course of the other initializations. 
   */ 
   for (k=0;k<23;k++)	/* VLCs of length <=9 are in rlev[0-22] */
   {  n = 8 - rlev[k][2];  /* # unused bits in the 9bit code */
      for (m=0; m < (1<<n) ; m++)
      {	 jmp_ac9dec[m | (rlev[k][3] << (n+1)) ] = k<<1;		     /* Pos. */
	 jmp_ac9dec[m | (rlev[k][3] << (n+1)) | (1<<n)] = (k<<1)+1;  /* Neg. */
      }
   }
   
   /* Initialize DOUBLE COEFF. entries.	  */
   for (k=0; k<18; k++)
   {  s1 = rlev[dbl[k][0]][2];
      s2 = rlev[dbl[k][1]][2];
      v1 = rlev[dbl[k][0]][3];
      v2 = rlev[dbl[k][1]][3];
      w = (v2<<(8-s1-s2)) | (v1<<(9-s1));
      for (m=0; m < (1 << (7-s1-s2)); m++) 
      {	 jmp_ac9dec[w | m] = (k<<2) + 46;			  /* (+,+) */
	 jmp_ac9dec[w | (1<< (7-s1-s2)) | m] = (k<<2) + 46 + 1;	  /* (+,-) */
	 jmp_ac9dec[w | (1<< (8-s1)) | m] = (k<<2) + 46 + 2;	  /* (+,-) */
	 /* (+,-) */
	 jmp_ac9dec[w | (1<< (8-s1)) | (1<< (7-s1-s2)) | m] = (k<<2) + 46 + 3;
      }
   }
   
   
   /* Initialize SINGLE COEFF. + EOB entries */
   for (k=0; k<11; k++)
   {  n = 9 - rlev[k][2]; 
      w = (rlev[k][3] << n) | (2<<(n-3)); 
      for (m=0; m < (1<<(n-3)) ; m++)
      {	 jmp_ac9dec[m | w ] = 118 + (k<<1);		     /* Pos. */
	 jmp_ac9dec[m | w | (1<<(n-1))] = 118 + (k<<1) + 1;  /* Neg. */
      }
   }
   
   
   /* Initialize DOUBLE COEFF. + EOB entries */
   for (k=0; k<3; k++)
   {  s1 = rlev[dbl_eob[k][0]][2];
      s2 = rlev[dbl_eob[k][1]][2];
      v1 = rlev[dbl_eob[k][0]][3];
      v2 = rlev[dbl_eob[k][1]][3];
      w = (v2<<(8-s1-s2)) | (v1<<(9-s1)) | (2<<(5-s1-s2));
      for (m=0; m < (1 << (5-s1-s2)); m++) 
      {	 jmp_ac9dec[w | m] = (k<<2) + 140;			  /* (+,+) */
	 jmp_ac9dec[w | (1<< (7-s1-s2)) | m] = (k<<2) + 140 + 1;  /* (+,-) */
	 jmp_ac9dec[w | (1<< (8-s1)) | m] = (k<<2) + 140 + 2;	  /* (+,-) */
	 /* (+,-) */
	 jmp_ac9dec[w | (1<< (8-s1)) | (1<< (7-s1-s2)) | m] = (k<<2) + 140 + 3;
      }
   }
   

   for (k=0;k<4;k++)		 /* "0000001xx" */
      jmp_ac9dec[4 | k] = 152;
   
   jmp_ac9dec[2] = 153;		 /* "00000001x" */
   jmp_ac9dec[3] = 153;
   
   jmp_ac9dec[1] = 154;		 /* "000000001" */
   jmp_ac9dec[0] = 155;		 /* "000000000" */
   
   for (k=0; k< (1<<7);k++) jmp_ac9dec[(2<<7) | k] = 156;   /* EOB */
   for (k=0; k< (1<<3);k++) jmp_ac9dec[(1<<3) | k] = 157;   /* ESC */
   
   printf("ubyte jmp_ac9dec[512] = {");
   for (k=0;k<512;k++) 
   {  if (! (k%16)) printf("\n      ");
      printf("%3d,",jmp_ac9dec[k]);
   }
   printf(" }\n");

/**********************************************************************************
   The following part creates the lk_chracskip[512] lookup table which is used to
   skip the ac coefficients of the chrominance blocks.
   The format of the table elements is as follows:
   
      sssss.ccc
   where ccc =	  
   0 :	 single or double coeff followed by EOB, or EOB alone,
	 sssss = # unused bits in the already read 9bits.
   4 :	 ESC, sssss = 0 (don't care)
   1 :	 <=9 #bits to be skipped corresponding single or double coeffs,
	 sssss = # unused bits in the already read 9bits.
   2 :	 known # more bits to be skipped in addition to the 9 already read bits,
	 sssss = deterministic # bits to be read and skipped.
   3 :	 unknown # more bits to be skipped, sssss = 0 (don't care).
**********************************************************************************/

   for (m=0;m<512;m++)
   {  k = jmp_ac9dec[m];
      if (k<46)		  /* Single_Coef vlc_size <=9 */
	 lk_chracskip[m] = ((8-rlev[k/2][2])<<3) | 1;
      
      
      else if (k<118)	  /* Double_Coef sum of vlc_sizes <=9 */
	 lk_chracskip[m] = ((7 - rlev[dbl[(k-46)/4][0]][2] - 
				 rlev[dbl[(k-46)/4][1]][2])<<3) | 1;	  
      
      
      else if (k<140)	  /* Single_Coef_EOB vlc_size+2 <=9 */
	 lk_chracskip[m] = ((6-rlev[(k-118)/2][2])<<3) | 0;
      
      
      else if (k<152)	  /* Double_Coef sum of vlc_sizes+2 <=9 */
	 lk_chracskip[m] = ((5 - rlev[dbl_eob[(k-140)/4][0]][2] - 
				 rlev[dbl_eob[(k-140)/4][1]][2])<<3) | 0;	  
      
      
      else if (k==152)	  /* Skip known #bits */
	 lk_chracskip[m] = (2<<3) | 2;
      else if (k==153)
	 lk_chracskip[m] = (4<<3) | 2;
      else if (k==154)
	 lk_chracskip[m] = (5<<3) | 2;
      else if (k==155)	  /* Skip more unknown #bits */
	 lk_chracskip[m] = 3 ;
      else if (k==156)	  /* EOB */
	 lk_chracskip[m] = (7<<3) | 0;
      else if (k==157)	  /* ESC */
	 lk_chracskip[m] = 4 ;
   } 
      
   printf("ubyte lk_chracskip[512] = {");
   for (k=0;k<512;k++) 
   {  if (! (k%16)) printf("\n      ");
      printf("%3d,",lk_chracskip[k]);
   }
   printf(" }\n");



/**************************************************************************
   The Following part prints the C source code which does the indirect 
   jumps by means of a switch statement. This ia as close as we can come
   to it with the help of a real good compiler.
**************************************************************************/


   for (k=0;k<23;k++)	
   {  printf("case %d:\n",2*k);
      printf("   ps += %d;\n",rlev[k][0]+1);
      printf("   cf = base[%d][ps];\n",rlev[k][1]-1);
      printf("   zzrmbits += %d;\n",8-rlev[k][2]);
      printf("   goto L_SingleCoef;\n");
      
      printf("case %d:\n",2*k+1);
      printf("   ps += %d;\n",rlev[k][0]+1);
      printf("   cf = -base[%d][ps];\n",rlev[k][1]-1);
      printf("   zzrmbits += %d;\n",8-rlev[k][2]);
      printf("   goto L_SingleCoef;\n");
   }
      
   for (k=0; k<18; k++)
   {  nint r0 = rlev[dbl[k][0]][0]+1;
      nint r1 = rlev[dbl[k][1]][0]+1;
      printf("case %d:\n",4*k+46);
      printf("   ps0 = ps + %d;\n",r0);
      printf("   ps = ps0 + %d;\n",r1);
      printf("   cf0 = base[%d][ps0];\n",rlev[dbl[k][0]][1]-1);
      printf("   cf = base[%d][ps0+%d];\n",rlev[dbl[k][1]][1]-1,r1);
      printf("   zzrmbits += %d;\n",7 - rlev[dbl[k][0]][2] - rlev[dbl[k][1]][2]);
      printf("   goto L_DoubleCoef;\n");
      
      printf("case %d:\n",4*k+46+1);
      printf("   ps0 = ps + %d;\n",r0);
      printf("   ps = ps0 + %d;\n",r1);
      printf("   cf0 = base[%d][ps0];\n",rlev[dbl[k][0]][1]-1);
      printf("   cf = -base[%d][ps0+%d];\n",rlev[dbl[k][1]][1]-1,r1);
      printf("   zzrmbits += %d;\n",7 - rlev[dbl[k][0]][2] - rlev[dbl[k][1]][2]);
      printf("   goto L_DoubleCoef;\n");
      
      printf("case %d:\n",4*k+46+2);
      printf("   ps0 = ps + %d;\n",r0);
      printf("   ps = ps0 + %d;\n",r1);
      printf("   cf0 = -base[%d][ps0];\n",rlev[dbl[k][0]][1]-1);
      printf("   cf = base[%d][ps0+%d];\n",rlev[dbl[k][1]][1]-1,r1);
      printf("   zzrmbits += %d;\n",7 - rlev[dbl[k][0]][2] - rlev[dbl[k][1]][2]);
      printf("   goto L_DoubleCoef;\n");
      
      printf("case %d:\n",4*k+46+3);
      printf("   ps0 = ps + %d;\n",r0);
      printf("   ps = ps0 + %d;\n",r1);
      printf("   cf0 = -base[%d][ps0];\n",rlev[dbl[k][0]][1]-1);
      printf("   cf = -base[%d][ps0+%d];\n",rlev[dbl[k][1]][1]-1, r1);
      printf("   zzrmbits += %d;\n",7 - rlev[dbl[k][0]][2] - rlev[dbl[k][1]][2]);
      printf("   goto L_DoubleCoef;\n");
   }
   
   for (k=0; k<11; k++)
   {  printf("case %d:\n",2*k+118);
      printf("   ps += %d;\n",rlev[k][0]+1);
      printf("   cf = base[%d][ps];\n",rlev[k][1]-1);
      printf("   zzrmbits += %d;\n",6-rlev[k][2]);
      printf("   goto L_SingleCoefEOB;\n");
      
      printf("case %d:\n",2*k+1+118);
      printf("   ps += %d;\n",rlev[k][0]+1);
      printf("   cf = -base[%d][ps];\n",rlev[k][1]-1);
      printf("   zzrmbits += %d;\n",6-rlev[k][2]);
      printf("   goto L_SingleCoefEOB;\n");
   }

   for (k=0; k<3; k++)
   {  nint r0 = rlev[dbl_eob[k][0]][0]+1;
      nint r1 = rlev[dbl_eob[k][1]][0]+1;
      
   
      printf("case %d:\n",4*k+140);
      printf("   ps0 = ps + %d;\n",r0);
      printf("   ps = ps0 + %d;\n",r1);
      printf("   cf0 = base[%d][ps0];\n",rlev[dbl_eob[k][0]][1]-1);
      printf("   cf = base[%d][ps0+%d];\n",rlev[dbl_eob[k][1]][1]-1,r1);
      printf("   zzrmbits += %d;\n",5 - rlev[dbl_eob[k][0]][2] - rlev[dbl_eob[k][1]][2]);
      printf("   goto L_DoubleCoefEOB;\n");
      
      printf("case %d:\n",4*k+140+1);
      printf("   ps0 = ps + %d;\n",r0);
      printf("   ps = ps0 + %d;\n",r1);
      printf("   cf0 = base[%d][ps0];\n",rlev[dbl_eob[k][0]][1]-1);
      printf("   cf = -base[%d][ps0+%d];\n",rlev[dbl_eob[k][1]][1]-1,r1);
      printf("   zzrmbits += %d;\n",5 - rlev[dbl_eob[k][0]][2] - rlev[dbl_eob[k][1]][2]);
      printf("   goto L_DoubleCoefEOB;\n");
      
      printf("case %d:\n",4*k+140+2);
      printf("   ps0 = ps + %d;\n",r0);
      printf("   ps = ps0 + %d;\n",r1);
      printf("   cf0 = -base[%d][ps0];\n",rlev[dbl_eob[k][0]][1]-1);
      printf("   cf = base[%d][ps0+%d];\n",rlev[dbl_eob[k][1]][1]-1,r1);
      printf("   zzrmbits += %d;\n",5 - rlev[dbl_eob[k][0]][2] - rlev[dbl_eob[k][1]][2]);
      printf("   goto L_DoubleCoefEOB;\n");
      
      printf("case %d:\n",4*k+140+3);
      printf("   ps0 = ps + %d;\n",r0);
      printf("   ps = ps0 + %d;\n",r1);
      printf("   cf0 = -base[%d][ps0];\n",rlev[dbl_eob[k][0]][1]-1);
      printf("   cf = -base[%d][ps0+%d];\n",rlev[dbl_eob[k][1]][1]-1,r1);
      printf("   zzrmbits += %d;\n",5 - rlev[dbl_eob[k][0]][2] - rlev[dbl_eob[k][1]][2]);
      printf("   goto L_DoubleCoefEOB;\n");
   }   
}




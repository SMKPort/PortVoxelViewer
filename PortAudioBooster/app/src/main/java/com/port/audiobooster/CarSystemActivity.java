package com.port.audiobooster;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.Locale;

/** Planejamento manual. Sem ler a bateria ou o clipping por sensores inexistentes. */
public final class CarSystemActivity extends Activity {
    private static final int BG=0xff0b090e, PANEL=0xff291f29, GOLD=0xffe5b456, WHITE=0xfff7eee6;
    private SharedPreferences prefs;
    private LinearLayout body;
    private EditText impedance,driverRms,amp1,amp2,voltageOff,voltageOn;
    private Spinner wiring;
    private TextView result;
    private int dp(int d){return Math.round(getResources().getDisplayMetrics().density*d);}
    private GradientDrawable rounded(int color){
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(12));return g;
    }
    private LinearLayout column(){LinearLayout c=new LinearLayout(this);c.setOrientation(1);return c;}
    private TextView text(String s,int size,int color){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;
    }
    private void pad(LinearLayout parent,int h){parent.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
    private LinearLayout section(String title){
        LinearLayout c=column();c.setPadding(dp(14),dp(14),dp(14),dp(14));
        c.setBackground(rounded(PANEL));
        body.addView(c,new LinearLayout.LayoutParams(-1,-2));
        pad(body,13);c.addView(text(title,18,GOLD));pad(c,10);return c;
    }
    private EditText field(LinearLayout c,String label,String key,String placeholder){
        c.addView(text(label,13,WHITE));EditText v=new EditText(this);
        v.setTextSize(16);v.setTextColor(WHITE);v.setHintTextColor(0xffa6959b);
        v.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        v.setHint(placeholder);
        String stored=prefs.getString(key,"");if(!stored.isEmpty())v.setText(stored);
        c.addView(v,new LinearLayout.LayoutParams(-1,dp(50)));pad(c,8);return v;
    }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        prefs=getSharedPreferences(GlobalAudioEngine.PREFS,MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        LinearLayout root=column();root.setPadding(dp(14),dp(18),dp(14),dp(12));root.setBackgroundColor(BG);
        setContentView(root);
        TextView heading=text("MEU PAREDÃO • DIAGNÓSTICO",22,GOLD);heading.setTypeface(null,Typeface.BOLD);
        root.addView(heading);pad(root,6);
        root.addView(text("Cálculo de ligação e registro de medidas. Não lê os módulos automaticamente.",12,WHITE));
        pad(root,13);
        ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        body=column();body.setPadding(0,0,0,dp(28));scroll.addView(body);

        LinearLayout setup=section("Sua configuração");
        setup.addView(text("4 falantes DUB de 12 polegadas, em duas caixas. Dois amplificadores anunciados como 600 W. Uma das caixas compartilha módulo com cornetas e tweeters. Confirme os detalhes antes de dimensionar.",13,WHITE));
        pad(setup,10);
        impedance=field(setup,"Impedância FINAL de cada falante (Ω)","car_driver_ohm","Ex.: 4");
        driverRms=field(setup,"RMS de cada falante (W)","car_driver_rms","Consultar etiqueta");
        amp1=field(setup,"RMS do módulo da caixa 1 (W)","car_amp1","Ex.: 600");
        amp2=field(setup,"RMS do módulo da caixa 2 (W)","car_amp2","Ex.: 600");

        LinearLayout conn=section("Ligação de 2 falantes por caixa");
        conn.addView(text("Escolha apenas se ambos são falantes iguais e conhece a impedância após eventual ligação das bobinas.",12,WHITE));
        wiring=new Spinner(this);wiring.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item,
            new String[]{"Não sei","Paralelo","Série"}));
        wiring.setSelection(prefs.getInt("car_wiring",0));conn.addView(wiring);

        LinearLayout measure=section("Tensão nos terminais dos módulos");
        measure.addView(text("Registre valores medidos com multímetro por alguém habilitado, sem mexer na instalação energizada ou nos terminais de potência durante o uso.",12,WHITE));
        voltageOff=field(measure,"Tensão do módulo com som baixo (V)","car_v_idle","Ex.: 12.4");
        voltageOn=field(measure,"Menor tensão durante graves (V)","car_v_bass","Ex.: 11.8");
        LinearLayout output=section("Interpretação");
        result=text("Preencha os dados que você conhece e calcule.",14,WHITE);output.addView(result);

        Button calc=new Button(this);calc.setAllCaps(false);calc.setText("Salvar e calcular");
        calc.setBackground(rounded(GOLD));calc.setTextColor(Color.BLACK);
        output.addView(calc,new LinearLayout.LayoutParams(-1,dp(52)));calc.setOnClickListener(v->compute());
        pad(output,10);
        output.addView(text("Não é medição de clipping nem análise de impedância mínima do amplificador. Se houver bobinas duplas, crossover ou canais separados, os cálculos precisam ser revisados.",12,0xffc6b7c0));
        compute();
    }
    private double parse(EditText e){
        try{String s=e.getText().toString().trim().replace(',','.');return s.isEmpty()?0:Double.parseDouble(s);}
        catch(NumberFormatException ignored){return 0;}
    }
    private void compute(){
        String[] keys={"car_driver_ohm","car_driver_rms","car_amp1","car_amp2","car_v_idle","car_v_bass"};
        EditText[] fields={impedance,driverRms,amp1,amp2,voltageOff,voltageOn};
        SharedPreferences.Editor ed=prefs.edit().putInt("car_wiring",wiring.getSelectedItemPosition());
        for(int i=0;i<keys.length;i++)ed.putString(keys[i],fields[i].getText().toString().trim());
        ed.apply();

        double ohm=parse(impedance),w=parse(driverRms),a1=parse(amp1),a2=parse(amp2);
        double vi=parse(voltageOff),vb=parse(voltageOn);
        int mode=wiring.getSelectedItemPosition();
        StringBuilder b=new StringBuilder();
        if(ohm>0&&mode!=0){
            double estimated=mode==1?ohm/2.0:ohm*2.0;
            b.append(String.format(Locale.US,"Carga teórica de cada caixa: %.2f Ω\n",estimated));
            b.append("Confira se o módulo suporta essa impedância por canal e como estão ligadas as bobinas.\n\n");
        }else b.append("Impedância da caixa: informe a ligação e os ohms finais dos falantes.\n\n");
        if(w>0){
            b.append(String.format(Locale.US,"2 falantes por caixa: %.0f W RMS nominais somados.\n",2*w));
            if(a1>0)b.append(String.format(Locale.US,"Caixa 1: módulo declarado %.0f W RMS, especificação na impedância usada ainda precisa ser conferida.\n",a1));
            if(a2>0)b.append(String.format(Locale.US,"Caixa 2: módulo declarado %.0f W RMS compartilhado com médios/agudos; falta divisão por canal.\n",a2));
            b.append("\n");
        }
        if(vi>0&&vb>0){
            double drop=vi-vb;
            b.append(String.format(Locale.US,"Queda de tensão informada: %.2f V.\n",drop));
            if(drop>0.5)b.append("Queda perceptível: confira cabos, conexões, alternador e alimentação com profissional.\n");
            else b.append("A medida isolada não descarta queda transitória ou clipping.\n");
        }
        b.append("\nDistorção existente antes do aplicativo pode vir do módulo, ajuste de ganho, ligação ou do próprio falante. Não compre módulo maior sem conferir modelo, ohms e alimentação.");
        result.setText(b.toString());
    }
}

package com.example.musicvibrationapp.emotion.audio;

import org.jtransforms.fft.FloatFFT_1D;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import static java.lang.Math.*;
import java.util.Arrays;

public class AudioAnalyzer {
    private static final int FRAME_SIZE = 2048;
    private static final int SAMPLING_RATE = 44100;
    private static final float MIN_FREQ = 20f;
    private static final int ENERGY_WINDOW = 10;
    
    private final FloatFFT_1D fft;
    private final float[] fftBuffer;
    private final float[] prevFrame;
    private final float[] spectrumBuffer;
    private final List<Float> energyHistory;
    private long lastBeatTime;
    private float prevBeat;
    
    private final float[] frequencyTable;
    
    private float[] inputBuffer;  // Add input buffer
    
    public AudioAnalyzer() {
        this.fft = new FloatFFT_1D(FRAME_SIZE);
        this.fftBuffer = new float[FRAME_SIZE];
        this.prevFrame = new float[FRAME_SIZE];
        this.spectrumBuffer = new float[FRAME_SIZE/2 + 1];
        this.energyHistory = new ArrayList<>();
        this.lastBeatTime = 0;
        this.prevBeat = 0;
        this.inputBuffer = new float[FRAME_SIZE];  // Initialize input buffer
        
        // Initialize frequency table
        this.frequencyTable = new float[FRAME_SIZE/2 + 1];
        for (int i = 0; i < frequencyTable.length; i++) {
            frequencyTable[i] = i * SAMPLING_RATE / (float)FRAME_SIZE;
        }
    }
    
    public synchronized AudioFeatures analyze(byte[] audioData) {
        // Convert audio data
        convertToFloats(audioData, fftBuffer);
        
        // Apply window function
        applyHannWindow(fftBuffer);
        
        // Perform FFT
        fft.realForward(fftBuffer);
        
        // Calculate spectrum
        float[] spectrum = computeSpectrum(fftBuffer);
        
        // Calculate basic features
        float amplitude = calculateAmplitude(spectrum);
        float energy = calculateEnergy(spectrum);
        float fundamentalFreq = detectFundamentalFrequency(spectrum);
        
        // Calculate harmonic content
        float[] harmonicContent = calculateHarmonics(spectrum, fundamentalFreq);
        
        // Calculate other features
        float centroid = calculateSpectralCentroid(spectrum);
        float spread = calculateSpectralSpread(spectrum, centroid);
        float rolloff = calculateSpectralRolloff(spectrum);
        float brightness = calculateBrightness(spectrum);
        float roughness = calculateRoughness(spectrum);
        float flux = calculateSpectralFlux();
        float harmonicComplexity = calculateHarmonicComplexity(harmonicContent);
        
        // Update state
        System.arraycopy(fftBuffer, 0, prevFrame, 0, FRAME_SIZE);
        
        return new AudioFeatures(
            amplitude,
            energy,
            fundamentalFreq,
            centroid,
            spread,
            rolloff,
            brightness,
            roughness,
            flux,
            harmonicComplexity,
            spectrum,
            harmonicContent,
            System.currentTimeMillis(),  // timestamp
            fundamentalFreq,             // pitch
            getPitchName(fundamentalFreq), // pitchName
            calculateLoudness(energy),   // loudness
            getDynamics(energy)          // dynamics
        );
    }
    
    private float[] computeSpectrum(float[] fftData) {
        // FFT result's complex data length is FRAME_SIZE
        // Real and imaginary parts are stored alternately, so spectrum length is FRAME_SIZE/2
        int spectrumLength = FRAME_SIZE/2;
        
        // 0th frequency component (DC component)
        spectrumBuffer[0] = Math.abs(fftData[0]);
        
        // Middle frequency components
        for (int i = 1; i < spectrumLength - 1; i++) {
            float re = fftData[2*i];
            float im = fftData[2*i + 1];
            spectrumBuffer[i] = (float) Math.sqrt(re * re + im * im);
        }
        
        // Highest frequency component (Nyquist frequency)
        spectrumBuffer[spectrumLength - 1] = Math.abs(fftData[1]);
        
        return spectrumBuffer;
    }
    
    private float detectFundamentalFrequency(float[] spectrum) {
        int startBin = Math.max(1, (int)(MIN_FREQ * FRAME_SIZE / SAMPLING_RATE));
        int maxBin = 0;
        float maxMagnitude = 0;
        
        for (int i = startBin; i < spectrum.length; i++) {
            if (spectrum[i] > maxMagnitude) {
                maxMagnitude = spectrum[i];
                maxBin = i;
            }
        }
        
        return frequencyTable[maxBin];
    }
    
    private boolean detectBeat(float energy) {
        energyHistory.add(energy);
        if (energyHistory.size() > ENERGY_WINDOW) {
            energyHistory.remove(0);
        }
        
        float avg = 0;
        for (float e : energyHistory) {
            avg += e;
        }
        avg /= energyHistory.size();
        
        float threshold = avg * 1.5f;
        long currentTime = System.currentTimeMillis();
        boolean isBeat = energy > threshold && 
                        currentTime - lastBeatTime > 200; // Minimum beat interval 200ms
        
        if (isBeat) {
            lastBeatTime = currentTime;
        }
        
        return isBeat;
    }
    
    public synchronized void reset() {
        Arrays.fill(fftBuffer, 0);
        Arrays.fill(prevFrame, 0);
        Arrays.fill(spectrumBuffer, 0);
        energyHistory.clear();
        lastBeatTime = 0;
        prevBeat = 0;
    }
    
    private void convertToFloats(byte[] audioData, float[] floatData) {
        float[] samples = byteToFloat(audioData);
        // If input data length is greater than frame size, only take frame size data
        int length = Math.min(samples.length, FRAME_SIZE);
        System.arraycopy(samples, 0, floatData, 0, length);
        // If data is insufficient, fill with 0
        if (length < FRAME_SIZE) {
            Arrays.fill(floatData, length, FRAME_SIZE, 0.0f);
        }
    }
    
    private float[] byteToFloat(byte[] audioData) {
        // Assume audio data is 16-bit PCM format
        float[] floatData = new float[audioData.length / 2];
        for (int i = 0; i < floatData.length; i++) {
            int sample = (audioData[2*i+1] << 8) | (audioData[2*i] & 0xFF);
            floatData[i] = sample / 32768.0f;
        }
        return floatData;
    }
    
    private float[] calculateSpectrum(float[] fftData) {
        float[] spectrum = new float[FRAME_SIZE/2];
        for (int i = 0; i < FRAME_SIZE/2; i++) {
            spectrum[i] = (float) sqrt(fftData[i*2] * fftData[i*2] + 
                                     fftData[i*2+1] * fftData[i*2+1]);
        }
        return spectrum;
    }
    
    private float calculateAmplitude(float[] samples) {
        float maxAmplitude = 0;
        for (float sample : samples) {
            maxAmplitude = max(maxAmplitude, abs(sample));
        }
        return maxAmplitude;
    }
    
    private float calculateEnergy(float[] samples) {
        float energy = 0;
        for (float sample : samples) {
            energy += sample * sample;
        }
        return energy / samples.length;
    }
    
    private float calculateSpectralCentroid(float[] spectrum) {
        float numerator = 0;
        float denominator = 0;
        float maxFreq = SAMPLING_RATE / 2.0f;
        
        for (int i = 0; i < FRAME_SIZE/2; i++) {
            float magnitude = spectrum[i];
            float frequency = i * SAMPLING_RATE / (float)FRAME_SIZE;
            numerator += frequency * magnitude;
            denominator += magnitude;
        }
        
        float centroid = denominator != 0 ? numerator / denominator : 0;
        return normalizeValue(centroid, maxFreq);
    }
    
    private float calculateSpectralSpread(float[] spectrum, float centroid) {
        float numerator = 0;
        float denominator = 0;
        
        for (int i = 0; i < FRAME_SIZE/2; i++) {
            float magnitude = spectrum[i];
            float frequency = i * SAMPLING_RATE / (float)FRAME_SIZE;
            numerator += (frequency - centroid) * (frequency - centroid) * magnitude;
            denominator += magnitude;
        }
        
        return denominator != 0 ? (float) sqrt(numerator / denominator) : 0;
    }
    
    private float calculateSpectralRolloff(float[] spectrum) {
        float totalEnergy = 0;
        float[] energies = new float[FRAME_SIZE/2];
        
        for (int i = 0; i < FRAME_SIZE/2; i++) {
            float magnitude = spectrum[i];
            energies[i] = magnitude;
            totalEnergy += magnitude;
        }
        
        float threshold = totalEnergy * 0.85f;
        float accumEnergy = 0;
        
        for (int i = 0; i < FRAME_SIZE/2; i++) {
            accumEnergy += energies[i];
            if (accumEnergy >= threshold) {
                return i * SAMPLING_RATE / (float)FRAME_SIZE;
            }
        }
        
        return SAMPLING_RATE / 2f;
    }
    
    private float calculateBrightness(float[] spectrum) {
        int cutoffBin = (int)(1500 * FRAME_SIZE / SAMPLING_RATE);
        float highFreqEnergy = 0;
        float totalEnergy = 0;
        
        for (int i = 0; i < FRAME_SIZE/2; i++) {
            float magnitude = spectrum[i];
            if (i >= cutoffBin) {
                highFreqEnergy += magnitude;
            }
            totalEnergy += magnitude;
        }
        
        return totalEnergy > 0 ? highFreqEnergy / totalEnergy : 0;
    }
    
    private float calculateRoughness(float[] spectrum) {
        float roughness = 0;
        float maxRoughness = 0;
        
        for (int i = 1; i < FRAME_SIZE/2; i++) {
            float mag1 = spectrum[i-1];
            float mag2 = spectrum[i];
            roughness += abs(mag1 - mag2);
            maxRoughness += max(mag1, mag2);
        }
        
        return maxRoughness > 0 ? roughness / maxRoughness : 0;
    }
    
    private float normalizeValue(float value, float maxValue) {
        return Math.min(1.0f, Math.max(0.0f, value / maxValue));
    }
    
    private float calculateSpectralFlux() {
        float flux = 0;
        for (int i = 0; i < FRAME_SIZE; i++) {
            float diff = fftBuffer[i] - prevFrame[i];
            flux += diff * diff;
        }
        System.arraycopy(fftBuffer, 0, prevFrame, 0, FRAME_SIZE);
        return flux;
    }
    
    private float calculateHarmonicComplexity(float[] harmonicContent) {
        float complexity = 0;
        float sum = 0;
        for (float magnitude : harmonicContent) {
            sum += magnitude;
        }
        if (sum > 0) {
            for (float magnitude : harmonicContent) {
                float p = magnitude / sum;
                if (p > 0) {
                    complexity -= p * Math.log(p) / Math.log(2);
                }
            }
        }
        return complexity / 4f; // Normalize to 0-1 range
    }
    
    private float calculateBeatStrength(float energy) {
        float threshold = 0.1f;
        return Math.max(0, Math.min(1, (energy - threshold) / (1 - threshold)));
    }

    private float[] calculateHarmonics(float[] spectrum, float fundamentalFreq) {
        float[] harmonics = new float[12];
        float maxMagnitude = 0;
        
        for (int i = 0; i < spectrum.length; i++) {
            float frequency = i * SAMPLING_RATE / (float)FRAME_SIZE;
            if (frequency > 0) {
                // Convert frequency to MIDI note symbol, using Math.log instead of log2
                float midiNote = 69 + 12 * (float)(Math.log(frequency/440) / Math.log(2));
                // Get pitch class (0-11)
                int pitchClass = ((int)Math.round(midiNote) % 12 + 12) % 12;
                harmonics[pitchClass] += spectrum[i];
                maxMagnitude = Math.max(maxMagnitude, harmonics[pitchClass]);
            }
        }
        
        // Normalize
        if (maxMagnitude > 0) {
            for (int i = 0; i < harmonics.length; i++) {
                harmonics[i] /= maxMagnitude;
            }
        }
        
        return harmonics;
    }

    private void applyHannWindow(float[] data) {
        for (int i = 0; i < FRAME_SIZE; i++) {
            float window = 0.5f * (1 - (float)cos(2 * Math.PI * i / (FRAME_SIZE - 1)));
            data[i] *= window;
        }
    }

    private float calculateTempo(boolean isBeat) {
        if (isBeat) {
            long currentTime = System.currentTimeMillis();
            if (lastBeatTime > 0) {
                float beatInterval = (currentTime - lastBeatTime) / 1000f; // Convert to seconds
                if (beatInterval > 0) {
                    return 60f / beatInterval; // Convert to BPM
                }
            }
        }
        return prevBeat;
    }

    private String getPitchName(float frequency) {
        if (frequency <= 0) return "";
        int midiNote = (int)(69 + 12 * Math.log(frequency / 440.0) / Math.log(2));
        int octave = (midiNote / 12) - 1;
        int noteIndex = midiNote % 12;
        String[] notes = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return notes[noteIndex] + octave;
    }

    private String getDynamics(float energy) {
        if (energy < 0.1f) return "pp";
        if (energy < 0.25f) return "p";
        if (energy < 0.4f) return "mp";
        if (energy < 0.6f) return "mf";
        if (energy < 0.8f) return "f";
        return "ff";
    }

    private float calculateLoudness(float energy) {
        return 20 * (float)Math.log10(Math.max(energy, 1e-6f));
    }

    private float calculateValence(float brightness, float harmonicComplexity, float energy) {
        return Math.min(1.0f, Math.max(0.0f, 
            brightness * 0.4f + (1.0f - harmonicComplexity) * 0.3f + energy * 0.3f));
    }

    private float calculateArousal(float energy, float brightness, float beatStrength) {
        return Math.min(1.0f, Math.max(0.0f,
            energy * 0.4f + brightness * 0.3f + beatStrength * 0.3f));
    }
} 
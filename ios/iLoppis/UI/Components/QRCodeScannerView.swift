import AVFoundation
import SwiftUI

struct QRCodeScannerView: UIViewControllerRepresentable {
    final class Coordinator: NSObject, AVCaptureMetadataOutputObjectsDelegate {
        var parent: QRCodeScannerView
        private var lastScanAt: Date?

        init(_ parent: QRCodeScannerView) {
            self.parent = parent
        }

        func metadataOutput(
            _ output: AVCaptureMetadataOutput,
            didOutput metadataObjects: [AVMetadataObject],
            from connection: AVCaptureConnection
        ) {
            guard parent.isRunning else { return }
            guard let object = metadataObjects.first as? AVMetadataMachineReadableCodeObject else { return }
            guard object.type == .qr else { return }
            guard let value = object.stringValue, !value.isEmpty else { return }

            // Simple debounce: avoid spamming repeated frames.
            let now = Date()
            if let last = lastScanAt, now.timeIntervalSince(last) < 1.0 { return }
            lastScanAt = now
            parent.onCode(value)
        }
    }

    let isRunning: Bool
    let onCode: (String) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    func makeUIViewController(context: Context) -> ScannerViewController {
        let controller = ScannerViewController()
        controller.onCode = onCode
        controller.delegate = context.coordinator
        controller.setRunning(isRunning)
        return controller
    }

    func updateUIViewController(_ uiViewController: ScannerViewController, context: Context) {
        uiViewController.onCode = onCode
        uiViewController.delegate = context.coordinator
        uiViewController.setRunning(isRunning)
    }
}

final class ScannerViewController: UIViewController {
    var onCode: ((String) -> Void)?
    weak var delegate: AVCaptureMetadataOutputObjectsDelegate?

    private let session = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var configured = false

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.bounds
    }

    func setRunning(_ running: Bool) {
        guard configured else {
            configureIfNeeded()
            // If configuration failed, don’t attempt start.
            if !configured { return }
            return setRunning(running)
        }
        if running {
            if !session.isRunning {
                DispatchQueue.global(qos: .userInitiated).async {
                    self.session.startRunning()
                }
            }
        } else {
            if session.isRunning {
                DispatchQueue.global(qos: .userInitiated).async {
                    self.session.stopRunning()
                }
            }
        }
    }

    private func configureIfNeeded() {
        guard !configured else { return }
        defer { configured = true }

        session.beginConfiguration()
        session.sessionPreset = .high

        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device),
              session.canAddInput(input) else {
            configured = false
            session.commitConfiguration()
            return
        }
        session.addInput(input)

        let output = AVCaptureMetadataOutput()
        guard session.canAddOutput(output) else {
            configured = false
            session.commitConfiguration()
            return
        }
        session.addOutput(output)
        output.setMetadataObjectsDelegate(delegate, queue: .main)
        output.metadataObjectTypes = [.qr]

        session.commitConfiguration()

        let preview = AVCaptureVideoPreviewLayer(session: session)
        preview.videoGravity = .resizeAspectFill
        preview.frame = view.bounds
        view.layer.addSublayer(preview)
        previewLayer = preview
    }
}
